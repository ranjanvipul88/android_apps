package com.ranjanvipul.relayguard.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ranjanvipul.relayguard.worker.NotificationEventWorker
import java.util.UUID

class RelayNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val request = OneTimeWorkRequestBuilder<NotificationEventWorker>()
            .setInputData(
                Data.Builder()
                    .putString(NotificationEventWorker.KEY_ID, UUID.randomUUID().toString())
                    .putString(NotificationEventWorker.KEY_PACKAGE, sbn.packageName)
                    .putString(NotificationEventWorker.KEY_TITLE, title)
                    .putString(NotificationEventWorker.KEY_TEXT, text)
                    .putLong(NotificationEventWorker.KEY_TIME, sbn.postTime)
                    .build()
            )
            .build()
        WorkManager.getInstance(applicationContext).enqueue(request)
    }
}
