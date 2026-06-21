package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SmsMessage as AndroidSmsMessage
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.Repository
import com.example.security.SmsSecurityAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import androidx.work.BackoffPolicy
import java.util.concurrent.TimeUnit

class SmsReceiver : BroadcastReceiver() {

    private val tag = "SmsReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // Extract SIM Slot / Subscription ID
        var slotIndex = intent.getIntExtra("android.telephony.extra.SLOT_INDEX", -1)
        if (slotIndex == -1) {
            slotIndex = intent.getIntExtra("subscription", -1)
        }

        val msgs: Array<AndroidSmsMessage>? = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse incoming SMS intent", e)
            null
        }

        if (msgs.isNullOrEmpty()) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context.applicationContext)
                val repository = Repository(db)



                val groupedMessages = mutableMapOf<String, StringBuilder>()
                for (msg in msgs) {
                    val sender = msg.originatingAddress ?: "Unknown"
                    val body = msg.messageBody ?: ""
                    groupedMessages.getOrPut(sender) { StringBuilder() }.append(body)
                }

                for ((sender, bodyBuilder) in groupedMessages) {
                    val body = bodyBuilder.toString()

                    if (body.isBlank()) continue

                    // 1. Locally encrypt and back up the SMS
                    repository.backUpMessage(sender, body)

                    // Log initial backup
                    repository.logAction(
                        sender = sender,
                        status = "BACKED_UP",
                        explanation = "Securely backed up encrypted message locally.",
                        destination = "None"
                    )

                    // 2. Look up forwarding rules
                    val activeRules = repository.getActiveRules()
                    var forwarded = false

                    for (rule in activeRules) {
                        val pattern = rule.contactPattern
                        val destination = rule.forwardToNumber

                        val match = if (pattern.equals("All Contacts/Numbers", ignoreCase = true) || pattern == "*" || pattern.isBlank()) {
                            true
                        } else {
                            sender.replace(" ", "").contains(pattern.replace(" ", ""))
                        }

                        if (match) {
                            // Check SIM Slot constraint
                            if (rule.simSlot != -1 && rule.simSlot != slotIndex) {
                                continue // Skip, wrong SIM
                            }

                            // 3. Security Exclusion Analysis (OTP and financial notification safety blocks)
                            val securityCheck = SmsSecurityAnalyzer.analyze(body)
                            val isBypassed = !securityCheck.isSafe && rule.allowBankingOtp

                            if (!securityCheck.isSafe && !rule.allowBankingOtp) {
                                repository.logAction(
                                    sender = sender,
                                    status = "BLOCKED_OTP",
                                    explanation = "Forwarding blocked. Rule matched, but message contains sensitive contents (Banking/OTP) and bypass is disabled: ${securityCheck.matchedPattern}",
                                    destination = destination
                                )
                                continue // Skip forwarding this message
                            }

                            // 4. Perform SMS Forwarding
                            if (destination.isNotBlank()) {
                                try {
                                    val smsManager: SmsManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        context.getSystemService(SmsManager::class.java)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        SmsManager.getDefault()
                                    }

                                    if (smsManager != null) {
                                        val prefix = "[Fwd from $sender]: "
                                        val obfuscatedBody = SmsSecurityAnalyzer.obfuscateForCarrierBypass(body)
                                        val fullMessage = prefix + obfuscatedBody
                                        val parts = smsManager.divideMessage(fullMessage)

                                        smsManager.sendMultipartTextMessage(destination, null, parts, null, null)

                                        val logExplanation = if (isBypassed) {
                                            "Successfully forwarded matching sensitive SMS (Banking/OTP bypassed: ${securityCheck.matchedPattern}) under filter: '${rule.label}'."
                                        } else {
                                            "Successfully forwarded matching SMS under filter: '${rule.label}'."
                                        }

                                        repository.logAction(
                                            sender = sender,
                                            status = "FORWARDED",
                                            explanation = logExplanation,
                                            destination = destination
                                        )
                                        forwarded = true
                                    } else {
                                        repository.logAction(
                                            sender = sender,
                                            status = "ERROR",
                                            explanation = "Failure forwarding: SmsManager unavailable on device.",
                                            destination = destination
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e(tag, "Error sending SMS forward", e)
                                    repository.logAction(
                                        sender = sender,
                                        status = "ERROR",
                                        explanation = "Forward failed: ${e.localizedMessage ?: "Unknown sending exception"}",
                                        destination = destination
                                    )
                                    
                                    if (rule.maxRetries > 0) {
                                        val fullMessage = "[Fwd from $sender]: " + SmsSecurityAnalyzer.obfuscateForCarrierBypass(body)
                                        val data = Data.Builder()
                                            .putString("payload", fullMessage)
                                            .putString("sender", sender)
                                            .putString("destination", destination)
                                            .putString("type", "SMS")
                                            .build()
                                        val request = OneTimeWorkRequestBuilder<ForwardingWorker>()
                                            .setInputData(data)
                                            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                                            .build()
                                        WorkManager.getInstance(context.applicationContext).enqueue(request)
                                    }
                                }
                            }

                            // 5. Perform Email Forwarding if enabled
                            if (rule.isEmailEnabled && rule.smtpSenderEmail.isNotBlank() && rule.smtpAppPassword.isNotBlank() && rule.targetEmail.isNotBlank()) {
                                val emailSuccess = com.example.data.EmailSender.sendEmail(
                                    senderEmail = rule.smtpSenderEmail,
                                    appPassword = rule.smtpAppPassword,
                                    targetEmail = rule.targetEmail,
                                    subject = "Forwarded SMS from $sender",
                                    bodyText = "Original sender: $sender\n\n$body\n\n--\nForwarded by SMS Safe Backup app."
                                )
                                repository.logAction(
                                    sender = sender,
                                    status = if (emailSuccess) "EMAIL_FORWARDED" else "EMAIL_ERROR",
                                    explanation = if (emailSuccess) "Successfully forwarded message to ${rule.targetEmail} via SMTP." else "Failed to send email to ${rule.targetEmail}. Check credentials or network connection.",
                                    destination = rule.targetEmail
                                )
                                if (!emailSuccess && rule.maxRetries > 0) {
                                    val data = Data.Builder()
                                        .putString("type", "EMAIL")
                                        .putString("sender", sender)
                                        .putString("payload", body)
                                        .putString("smtpSenderEmail", rule.smtpSenderEmail)
                                        .putString("smtpAppPassword", rule.smtpAppPassword)
                                        .putString("targetEmail", rule.targetEmail)
                                        .build()
                                    val request = OneTimeWorkRequestBuilder<ForwardingWorker>()
                                        .setInputData(data)
                                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                                        .build()
                                    WorkManager.getInstance(context.applicationContext).enqueue(request)
                                }
                                forwarded = true
                            }

                            // 6. Perform Telegram Forwarding if enabled
                            if (rule.isTelegramEnabled && rule.telegramBotToken.isNotBlank() && rule.telegramChatId.isNotBlank()) {
                                val telegramSuccess = com.example.data.TelegramSender.sendMessage(
                                    botToken = rule.telegramBotToken,
                                    chatId = rule.telegramChatId,
                                    text = "📱 *Forwarded SMS*\nFrom: `$sender`\n\n$body"
                                )
                                repository.logAction(
                                    sender = sender,
                                    status = if (telegramSuccess) "TELEGRAM_FORWARDED" else "TELEGRAM_ERROR",
                                    explanation = if (telegramSuccess) "Successfully forwarded message to Telegram." else "Failed to send to Telegram. Check your Bot Token and Chat ID.",
                                    destination = "Telegram"
                                )
                                if (!telegramSuccess && rule.maxRetries > 0) {
                                    val data = Data.Builder()
                                        .putString("type", "TELEGRAM")
                                        .putString("sender", sender)
                                        .putString("payload", body)
                                        .putString("telegramBotToken", rule.telegramBotToken)
                                        .putString("telegramChatId", rule.telegramChatId)
                                        .build()
                                    val request = OneTimeWorkRequestBuilder<ForwardingWorker>()
                                        .setInputData(data)
                                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                                        .build()
                                    WorkManager.getInstance(context.applicationContext).enqueue(request)
                                }
                                forwarded = true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error inside background SmsReceiver execution", e)
            } finally {
                val prefs = context.getSharedPreferences("sms_backup_prefs", Context.MODE_PRIVATE)
                prefs.edit().putLong("last_sync_timestamp", System.currentTimeMillis()).apply()
                pendingResult.finish()
            }
        }
    }
}
