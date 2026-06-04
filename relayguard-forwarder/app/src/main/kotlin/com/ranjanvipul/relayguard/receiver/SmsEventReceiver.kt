package com.ranjanvipul.relayguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ranjanvipul.relayguard.worker.InboundMessageWorker
import java.util.UUID

class SmsEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val sender = messages.firstOrNull()?.originatingAddress.orEmpty()
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val timestamp = messages.minOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()

        val request = OneTimeWorkRequestBuilder<InboundMessageWorker>()
            .setInputData(
                Data.Builder()
                    .putString(InboundMessageWorker.KEY_ID, UUID.randomUUID().toString())
                    .putString(InboundMessageWorker.KEY_SENDER, sender)
                    .putString(InboundMessageWorker.KEY_BODY, body)
                    .putLong(InboundMessageWorker.KEY_TIME, timestamp)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
