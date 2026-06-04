package com.ranjanvipul.relayguard.domain.repository

import com.ranjanvipul.relayguard.domain.model.ForwardFilter
import com.ranjanvipul.relayguard.domain.model.MessageEvent
import com.ranjanvipul.relayguard.domain.model.RelayAttempt
import com.ranjanvipul.relayguard.domain.model.RenderedRelay
import kotlinx.coroutines.flow.Flow

interface FilterRepository {
    fun observeFilters(): Flow<List<ForwardFilter>>
    suspend fun saveFilter(filter: ForwardFilter)
    suspend fun setFilterEnabled(filterId: String, enabled: Boolean)
    suspend fun deleteFilter(filterId: String)
}

interface MessageLogRepository {
    fun observeRecentEvents(limit: Int = 100): Flow<List<MessageEvent>>
    fun observeRecentRelays(limit: Int = 100): Flow<List<RelayAttempt>>
    suspend fun recordEvent(event: MessageEvent)
    suspend fun recordRelay(relay: RenderedRelay, success: Boolean, detail: String?)
}

interface RelayTransport {
    suspend fun send(relay: RenderedRelay): Result<Unit>
}
