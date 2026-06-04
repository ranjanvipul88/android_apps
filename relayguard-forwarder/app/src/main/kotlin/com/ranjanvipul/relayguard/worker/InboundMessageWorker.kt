package com.ranjanvipul.relayguard.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ranjanvipul.relayguard.domain.model.MessageEvent
import com.ranjanvipul.relayguard.domain.model.MessageKind
import com.ranjanvipul.relayguard.domain.repository.FilterRepository
import com.ranjanvipul.relayguard.domain.repository.MessageLogRepository
import com.ranjanvipul.relayguard.domain.repository.RelayTransport
import com.ranjanvipul.relayguard.domain.usecase.EvaluateMessageUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class InboundMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val filters: FilterRepository,
    private val messages: MessageLogRepository,
    private val transport: RelayTransport,
    private val evaluate: EvaluateMessageUseCase
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val event = MessageEvent(
            id = inputData.getString(KEY_ID) ?: return Result.failure(),
            kind = MessageKind.IncomingSms,
            sender = inputData.getString(KEY_SENDER).orEmpty(),
            body = inputData.getString(KEY_BODY).orEmpty(),
            receivedAtEpochMillis = inputData.getLong(KEY_TIME, System.currentTimeMillis())
        )
        messages.recordEvent(event)
        val relays = evaluate.evaluate(event, filters.observeFilters().first())
        relays.forEach { relay ->
            val result = transport.send(relay)
            messages.recordRelay(relay, result.isSuccess, result.exceptionOrNull()?.message)
        }
        return Result.success()
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_SENDER = "sender"
        const val KEY_BODY = "body"
        const val KEY_TIME = "time"
    }
}
