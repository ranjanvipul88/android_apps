package com.example.whatsappwebnative

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.JavascriptInterface
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class WebAppInterface(
    private val context: Context,
    private val notificationPermissionRequester: (() -> Unit)? = null
) {

    init {
        ensureNotificationChannel()
    }

    @JavascriptInterface
    fun showNotification(title: String?, body: String?, tag: String?) {
        MessageRecoveryStore.add(context, title, body, tag)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title?.takeIf { it.isNotBlank() } ?: "WhatsApp Web")
            .setContentText(body.orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body.orEmpty()))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = (tag ?: "${title.orEmpty()}:${body.orEmpty()}").hashCode()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    @JavascriptInterface
    fun requestNotificationPermission() {
        notificationPermissionRequester?.invoke()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "ChatDock Web notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Native notifications forwarded from official web chat sessions."
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "whatsapp_web_notifications"
        const val JS_NAME = "AndroidWhatsAppBridge"
    }
}
