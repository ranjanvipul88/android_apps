package com.ranjanvipul.relayguard.worker

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ranjanvipul.relayguard.domain.model.ForwardFilter
import com.ranjanvipul.relayguard.domain.model.MessageEvent
import com.ranjanvipul.relayguard.domain.model.RenderedRelay
import com.ranjanvipul.relayguard.domain.repository.FilterRepository
import com.ranjanvipul.relayguard.domain.repository.MessageLogRepository
import com.ranjanvipul.relayguard.domain.repository.RelayTransport
import com.ranjanvipul.relayguard.domain.usecase.EvaluateMessageUseCase
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class RelayDispatcher @Inject constructor(
    private val filters: FilterRepository,
    private val messages: MessageLogRepository,
    private val transport: RelayTransport,
    private val evaluate: EvaluateMessageUseCase,
    private val duplicateGuard: DuplicateGuard
) {
    suspend fun dispatch(event: MessageEvent) {
        messages.recordEvent(event)
        filters.observeFilters().first()
            .filter { it.enabled && event.kind in it.messageKinds }
            .forEach { filter -> dispatchForFilter(event, filter) }
    }

    private suspend fun dispatchForFilter(event: MessageEvent, filter: ForwardFilter) {
        if (filter.preventRapidDuplicates && duplicateGuard.isDuplicate(event.fingerprint(filter.id))) {
            return
        }

        val relays = evaluate.evaluate(event, listOf(filter))
        val primary = relays.filterNot { it.recipient.isFallback }
        val fallback = relays.filter { it.recipient.isFallback }
        val primaryResults = primary.map { relay -> relay to send(relay) }

        val shouldUseFallback = primary.isEmpty() || primaryResults.any { !it.second.isSuccess }
        if (shouldUseFallback) {
            fallback.forEach { relay -> send(relay) }
        }

        if (relays.isNotEmpty()) {
            duplicateGuard.markSeen(event.fingerprint(filter.id))
        }
    }

    private suspend fun send(relay: RenderedRelay): Result<Unit> {
        val result = transport.send(relay)
        messages.recordRelay(relay, result.isSuccess, result.exceptionOrNull()?.message)
        return result
    }

    private fun MessageEvent.fingerprint(filterId: String): String = sha256(
        listOf(filterId, kind.name, sender, body, simSlot.orEmpty(), packageName.orEmpty()).joinToString("\u001f")
    )

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}

@Singleton
class DuplicateGuard @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("relayguard_duplicate_guard", Context.MODE_PRIVATE)
    private val windowMillis = 120_000L

    fun isDuplicate(fingerprint: String, now: Long = System.currentTimeMillis()): Boolean {
        trim(now)
        val seenAt = prefs.getLong(fingerprint, 0L)
        return seenAt > 0L && now - seenAt <= windowMillis
    }

    fun markSeen(fingerprint: String, now: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(fingerprint, now).apply()
    }

    private fun trim(now: Long) {
        val stale = prefs.all
            .filterValues { value -> value is Long && now - value > windowMillis }
            .keys
        if (stale.isNotEmpty()) {
            prefs.edit().apply {
                stale.forEach(::remove)
            }.apply()
        }
    }
}
