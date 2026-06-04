package com.ranjanvipul.relayguard.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ranjanvipul.relayguard.domain.model.MessageEvent
import com.ranjanvipul.relayguard.domain.model.MessageKind
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class InboundMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dispatcher: RelayDispatcher
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val event = MessageEvent(
            id = inputData.getString(KEY_ID) ?: return Result.failure(),
            kind = inputData.getString(KEY_KIND)?.let(MessageKind::valueOf) ?: MessageKind.IncomingSms,
            sender = inputData.getString(KEY_SENDER).orEmpty(),
            body = inputData.getString(KEY_BODY).orEmpty(),
            receivedAtEpochMillis = inputData.getLong(KEY_TIME, System.currentTimeMillis()),
            simSlot = inputData.getString(KEY_SIM_SLOT),
            attachmentCount = inputData.getInt(KEY_ATTACHMENT_COUNT, 0)
        )
        dispatcher.dispatch(event)
        return Result.success()
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_SENDER = "sender"
        const val KEY_BODY = "body"
        const val KEY_TIME = "time"
        const val KEY_KIND = "kind"
        const val KEY_SIM_SLOT = "simSlot"
        const val KEY_ATTACHMENT_COUNT = "attachmentCount"
    }
}
