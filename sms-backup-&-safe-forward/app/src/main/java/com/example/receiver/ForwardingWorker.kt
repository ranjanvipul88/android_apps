package com.example.receiver

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.Repository
import com.example.data.TelegramSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ForwardingWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val payload = inputData.getString("payload") ?: return@withContext Result.failure()
        val sender = inputData.getString("sender") ?: "Unknown"
        val destination = inputData.getString("destination") ?: "Unknown"
        val type = inputData.getString("type") ?: "TELEGRAM"

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = Repository(db)

        try {
            var success = false
            if (type == "TELEGRAM") {
                val botToken = inputData.getString("telegramBotToken") ?: ""
                val chatId = inputData.getString("telegramChatId") ?: ""

                if (botToken.isNotBlank() && chatId.isNotBlank()) {
                    val formattedPayload = "📱 *Retried Forwarded SMS*\nFrom: `$sender`\n\n$payload"
                    success = TelegramSender.sendMessage(botToken, chatId, formattedPayload)
                }
            } else if (type == "EMAIL") {
                val smtpSender = inputData.getString("smtpSenderEmail") ?: ""
                val smtpPassword = inputData.getString("smtpAppPassword") ?: ""
                val targetEmail = inputData.getString("targetEmail") ?: ""
                
                if (smtpSender.isNotBlank() && smtpPassword.isNotBlank() && targetEmail.isNotBlank()) {
                    success = com.example.data.EmailSender.sendEmail(
                        senderEmail = smtpSender,
                        appPassword = smtpPassword,
                        targetEmail = targetEmail,
                        subject = "Retried Forwarded SMS from $sender",
                        bodyText = payload
                    )
                }
            } else if (type == "SMS") {
                val smsManager: android.telephony.SmsManager? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    applicationContext.getSystemService(android.telephony.SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    android.telephony.SmsManager.getDefault()
                }

                if (smsManager != null && destination.isNotBlank()) {
                    val parts = smsManager.divideMessage(payload)
                    smsManager.sendMultipartTextMessage(destination, null, parts, null, null)
                    success = true
                }
            }

            if (success) {
                repository.logAction(
                    sender = sender,
                    status = "FORWARDED_RETRY_SUCCESS",
                    explanation = "Successfully forwarded message after background retry via $type.",
                    destination = destination
                )
                return@withContext Result.success()
            } else {
                repository.logAction(
                    sender = sender,
                    status = "FORWARDED_RETRY_FAIL",
                    explanation = "Failed forwarding via $type during retry. Will retry if limit not reached.",
                    destination = destination
                )
                return@withContext Result.retry()
            }
        } catch (e: Exception) {
            repository.logAction(
                sender = sender,
                status = "WORKER_ERROR",
                explanation = "Worker crashed: ${e.message}",
                destination = "N/A"
            )
            return@withContext Result.retry()
        }
    }
}
