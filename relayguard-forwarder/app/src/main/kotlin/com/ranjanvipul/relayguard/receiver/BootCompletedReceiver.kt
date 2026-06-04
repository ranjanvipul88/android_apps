package com.ranjanvipul.relayguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ranjanvipul.relayguard.worker.RelayWorkScheduler
import com.ranjanvipul.relayguard.worker.StartupAuditWorker

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "relayguard-startup-audit",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<StartupAuditWorker>().build()
            )
            RelayWorkScheduler.scheduleOutgoingScans(context)
            RelayWorkScheduler.runOutgoingScanNow(context)
        }
    }
}
