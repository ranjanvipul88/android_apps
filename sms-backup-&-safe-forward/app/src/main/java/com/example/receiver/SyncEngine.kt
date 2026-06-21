package com.example.receiver

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.EmailSender
import com.example.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

object SyncEngine {
    private const val TAG = "SyncEngine"

    suspend fun runCatchUpSync(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("sms_backup_prefs", Context.MODE_PRIVATE)
        val lastSyncTime = prefs.getLong("last_sync_timestamp", 0L)
        val now = System.currentTimeMillis()

        if (lastSyncTime == 0L) {
            // First run, just set the checkpoint
            prefs.edit().putLong("last_sync_timestamp", now).apply()
            return@withContext
        }

        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return@withContext
        }

        val db = AppDatabase.getDatabase(context)
        val repository = Repository(db)
        val rules = repository.allRules.firstOrNull() ?: emptyList()
        val activeRules = rules.filter { it.isEnabled }

        if (activeRules.isEmpty()) {
            prefs.edit().putLong("last_sync_timestamp", now).apply()
            return@withContext
        }

        val missedMessages = mutableListOf<Triple<String, String, Long>>()

        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("address", "body", "date")
        val selection = "date > ?"
        val selectionArgs = arrayOf(lastSyncTime.toString())

        context.contentResolver.query(uri, projection, selection, selectionArgs, "date ASC")?.use { cursor ->
            val addressIndex = cursor.getColumnIndex("address")
            val bodyIndex = cursor.getColumnIndex("body")
            val dateIndex = cursor.getColumnIndex("date")

            while (cursor.moveToNext()) {
                val address = cursor.getString(addressIndex) ?: "Unknown"
                val body = cursor.getString(bodyIndex) ?: ""
                val date = cursor.getLong(dateIndex)
                missedMessages.add(Triple(address, body, date))
            }
        }

        if (missedMessages.isEmpty()) {
            prefs.edit().putLong("last_sync_timestamp", now).apply()
            return@withContext
        }

        // Filter messages based on active rules
        val pendingToForwardByRule = mutableMapOf<com.example.data.ForwardingRule, MutableList<Pair<String, String>>>()
        var latestMessageTime = lastSyncTime

        for ((sender, body, date) in missedMessages) {
            if (date > latestMessageTime) {
                latestMessageTime = date
            }

            for (rule in activeRules) {
                if (rule.contactPattern == "All Contacts/Numbers" || sender.contains(rule.contactPattern)) {
                    val isBankingOtp = body.contains("OTP", ignoreCase = true) || body.contains("Bank", ignoreCase = true)
                    if (isBankingOtp && !rule.allowBankingOtp) continue
                    
                    pendingToForwardByRule.getOrPut(rule) { mutableListOf() }.add(Pair(sender, body))
                }
            }
        }

        if (pendingToForwardByRule.isEmpty()) {
            prefs.edit().putLong("last_sync_timestamp", latestMessageTime).apply()
            return@withContext
        }

        // Collation logic
        for ((rule, messages) in pendingToForwardByRule) {
            if (messages.size > 35) {
                if (rule.isEmailEnabled && rule.smtpSenderEmail.isNotBlank() && rule.smtpAppPassword.isNotBlank()) {
                    val collatedBody = StringBuilder()
                    collatedBody.append("🚨 Catch-Up Sync Triggered! 🚨\n\n")
                    collatedBody.append("You missed ${messages.size} messages for filter '${rule.label}'.\n\n")
                    
                    messages.forEachIndexed { index, pair ->
                        collatedBody.append("${index + 1}. From: ${pair.first}\n${pair.second}\n\n")
                    }

                    val success = EmailSender.sendEmail(
                        senderEmail = rule.smtpSenderEmail,
                        appPassword = rule.smtpAppPassword,
                        targetEmail = rule.targetEmail,
                        subject = "Collated Catch-Up SMS (${messages.size} messages)",
                        bodyText = collatedBody.toString()
                    )

                    if (success) {
                        repository.logAction(
                            sender = "SyncEngine",
                            status = "SYNC_COLLATED_SUCCESS",
                            explanation = "Collated ${messages.size} messages into a single email for rule '${rule.label}'.",
                            destination = rule.targetEmail
                        )
                    } else {
                        repository.logAction(
                            sender = "SyncEngine",
                            status = "SYNC_COLLATED_FAIL",
                            explanation = "Failed to send collated email payload for rule '${rule.label}'.",
                            destination = rule.targetEmail
                        )
                    }
                }
            } else {
                // Process normally via WorkManager or Email
                for ((sender, body) in messages) {
                    repository.logAction(
                        sender = sender,
                        status = "SYNC_INDIVIDUAL",
                        explanation = "Recovered missed message during sync for rule '${rule.label}'.",
                        destination = "System"
                    )
                }
            }
        }

        prefs.edit().putLong("last_sync_timestamp", latestMessageTime).apply()
    }
}
