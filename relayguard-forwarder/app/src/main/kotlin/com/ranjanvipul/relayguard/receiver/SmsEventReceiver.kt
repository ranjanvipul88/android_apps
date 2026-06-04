package com.ranjanvipul.relayguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ranjanvipul.relayguard.domain.model.MessageKind
import com.ranjanvipul.relayguard.worker.InboundMessageWorker
import java.util.UUID

class SmsEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> enqueueSms(context, intent)
            Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION -> enqueueMmsNotice(context, intent)
            ACTION_RCS_RECEIVED -> enqueueRcs(context, intent)
        }
    }

    private fun enqueueSms(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return
        val sender = messages.firstOrNull()?.originatingAddress.orEmpty()
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val timestamp = messages.minOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()
        enqueue(
            context = context,
            kind = MessageKind.IncomingSms.name,
            sender = sender,
            body = body,
            timestamp = timestamp,
            simSlot = intent.getIntExtra("slot", intent.getIntExtra("simSlot", -1)).takeIf { it >= 0 }?.toString()
        )
    }

    private fun enqueueMmsNotice(context: Context, intent: Intent) {
        enqueue(
            context = context,
            kind = MessageKind.IncomingMmsNotice.name,
            sender = intent.getStringExtra("address").orEmpty(),
            body = "MMS notification received",
            timestamp = System.currentTimeMillis(),
            simSlot = intent.getIntExtra("slot", -1).takeIf { it >= 0 }?.toString(),
            attachmentCount = 1
        )
    }

    private fun enqueueRcs(context: Context, intent: Intent) {
        val body = intent.getStringExtra("body")
            ?: intent.getStringExtra("text")
            ?: intent.getStringExtra("message")
            ?: "RCS message received"
        enqueue(
            context = context,
            kind = MessageKind.IncomingRcs.name,
            sender = intent.getStringExtra("sender").orEmpty(),
            body = body,
            timestamp = System.currentTimeMillis(),
            simSlot = intent.getIntExtra("slot", -1).takeIf { it >= 0 }?.toString()
        )
    }

    private fun enqueue(
        context: Context,
        kind: String,
        sender: String,
        body: String,
        timestamp: Long,
        simSlot: String?,
        attachmentCount: Int = 0
    ) {
        val request = OneTimeWorkRequestBuilder<InboundMessageWorker>()
            .setInputData(
                Data.Builder()
                    .putString(InboundMessageWorker.KEY_ID, UUID.randomUUID().toString())
                    .putString(InboundMessageWorker.KEY_KIND, kind)
                    .putString(InboundMessageWorker.KEY_SENDER, sender)
                    .putString(InboundMessageWorker.KEY_BODY, body)
                    .putLong(InboundMessageWorker.KEY_TIME, timestamp)
                    .putString(InboundMessageWorker.KEY_SIM_SLOT, simSlot)
                    .putInt(InboundMessageWorker.KEY_ATTACHMENT_COUNT, attachmentCount)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    companion object {
        private const val ACTION_RCS_RECEIVED = "com.services.rcs.MESSAGE_RECEIVED"
    }
}
