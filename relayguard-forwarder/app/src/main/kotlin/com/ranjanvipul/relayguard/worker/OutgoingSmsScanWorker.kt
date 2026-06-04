package com.ranjanvipul.relayguard.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ranjanvipul.relayguard.domain.model.MessageEvent
import com.ranjanvipul.relayguard.domain.model.MessageKind
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class OutgoingSmsScanWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val dispatcher: RelayDispatcher
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return Result.success()
        }

        val prefs = context.getSharedPreferences("relayguard_outgoing_scan", Context.MODE_PRIVATE)
        val lastSeen = prefs.getLong(KEY_LAST_SEEN_DATE, 0L)
        var newest = lastSeen

        context.contentResolver.query(
            Telephony.Sms.Sent.CONTENT_URI,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            "${Telephony.Sms.DATE} > ?",
            arrayOf(lastSeen.toString()),
            "${Telephony.Sms.DATE} ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val date = cursor.getLong(dateIndex)
                newest = maxOf(newest, date)
                dispatcher.dispatch(
                    MessageEvent(
                        id = "outgoing-$id",
                        kind = MessageKind.OutgoingSms,
                        sender = cursor.getString(addressIndex).orEmpty(),
                        body = cursor.getString(bodyIndex).orEmpty(),
                        receivedAtEpochMillis = date
                    )
                )
            }
        }

        if (newest > lastSeen) {
            prefs.edit().putLong(KEY_LAST_SEEN_DATE, newest).apply()
        }
        return Result.success()
    }

    companion object {
        private const val KEY_LAST_SEEN_DATE = "lastSeenDate"
    }
}
