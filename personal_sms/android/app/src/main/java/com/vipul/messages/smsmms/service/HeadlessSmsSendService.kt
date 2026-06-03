package com.vipul.messages.smsmms.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Mandatory headless service for Default SMS Handler designation.
 * Handles system quick-replies (e.g. from lockscreen or notifications).
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Quick responses triggered silently
        return START_NOT_STICKY
    }
}
