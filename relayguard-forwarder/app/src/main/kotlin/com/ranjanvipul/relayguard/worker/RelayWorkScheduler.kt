package com.ranjanvipul.relayguard.worker

import android.content.Context
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object RelayWorkScheduler {
    fun runOutgoingScanNow(context: Context) {
        workManager(context).enqueueUniqueWork(
            "relayguard-outgoing-scan-now",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<OutgoingSmsScanWorker>().build()
        )
    }

    fun scheduleOutgoingScans(context: Context) {
        workManager(context).enqueueUniquePeriodicWork(
            "relayguard-outgoing-scan",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<OutgoingSmsScanWorker>(15, TimeUnit.MINUTES).build()
        )
    }

    private fun workManager(context: Context): WorkManager {
        val appContext = context.applicationContext
        return runCatching { WorkManager.getInstance(appContext) }.getOrElse {
            val config = (appContext as? Configuration.Provider)?.workManagerConfiguration
                ?: Configuration.Builder().build()
            runCatching { WorkManager.initialize(appContext, config) }
            WorkManager.getInstance(appContext)
        }
    }
}
