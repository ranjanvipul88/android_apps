package com.vipul.messages.smsmms.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vipul.messages.smsmms.MainActivity
import com.vipul.messages.smsmms.data.SmsDatabase
import com.vipul.messages.smsmms.data.SmsMessage as LocalSms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class SmsReceiver : BroadcastReceiver() {

    private val TAG = "SmsReceiver"
    private val CHANNEL_ID = "IncomingSmsChannel"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_DELIVER" || 
            intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            
            val bundle = intent.extras
            if (bundle != null) {
                try {
                    val pdus = bundle.get("pdus") as Array<*>
                    val format = bundle.getString("format")
                    
                    for (i in pdus.indices) {
                        val sms: SmsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            SmsMessage.createFromPdu(pdus[i] as ByteArray, format)
                        } else {
                            @Suppress("DEPRECATION")
                            SmsMessage.createFromPdu(pdus[i] as ByteArray)
                        }

                        val senderPhone = sms.originatingAddress ?: "Unknown"
                        val messageBody = sms.messageBody ?: ""
                        val timestamp = sms.timestampMillis

                        Log.d(TAG, "Received incoming SMS from $senderPhone: $messageBody")

                        // Process incoming SMS in IO Scope
                        processReceivedSms(context, senderPhone, messageBody, timestamp)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing incoming SMS PDU", e)
                }
            }
        }
    }

    private fun processReceivedSms(context: Context, sender: String, body: String, timestamp: Long) {
        val database = SmsDatabase.getDatabase(context)
        val sharedPrefs = context.getSharedPreferences("sms_replica_prefs", Context.MODE_PRIVATE)
        
        // 1. Core local storage caching
        val localMsg = LocalSms(
            message_id = "SMS-${UUID.randomUUID()}",
            phone_number = sender,
            sender_name = sender, // Can resolve via contacts provider in production
            body = body,
            timestamp = timestamp,
            direction = "incoming",
            type = "sms",
            mms_attachment_url = "",
            is_starred = false,
            is_synced = false
        )

        CoroutineScope(Dispatchers.IO).launch {
            // Save to database
            database.smsDao().insertMessage(localMsg)

            // 2. Perform Background Auto-Forwarding Check
            val isForwardEnabled = sharedPrefs.getBoolean("forward_enabled", false)
            val forwardNumber = sharedPrefs.getString("forward_phone", "") ?: ""

            if (isForwardEnabled && forwardNumber.isNotEmpty()) {
                Log.d(TAG, "Auto-Forward is ACTIVE. Forwarding text to $forwardNumber")
                try {
                    val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        context.getSystemService(SmsManager::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsManager.getDefault()
                    }
                    
                    val forwardPayload = "[FWD from $sender]: $body"
                    
                    // Split SMS in case it is too long
                    val parts = smsManager.divideMessage(forwardPayload)
                    smsManager.sendMultipartTextMessage(forwardNumber, null, parts, null, null)
                    
                    // Save auto-forwarded text to local DB as outgoing
                    val fwdOutgoing = LocalSms(
                        message_id = "FWD-OUT-${UUID.randomUUID()}",
                        phone_number = forwardNumber,
                        sender_name = "Auto-Forwarder",
                        body = forwardPayload,
                        timestamp = System.currentTimeMillis(),
                        direction = "outgoing",
                        type = "sms"
                    )
                    database.smsDao().insertMessage(fwdOutgoing)
                    Log.d(TAG, "Auto-forwarded message saved to local logs.")

                } catch (e: Exception) {
                    Log.e(TAG, "Background auto-forwarding failed to execute", e)
                }
            }

            // 3. Launch System alert notifications
            showSmsNotification(context, sender, body)
        }
    }

    private fun showSmsNotification(context: Context, sender: String, body: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SMS/MMS Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Triggers alerts when SMS are received"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("New SMS: $sender")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
