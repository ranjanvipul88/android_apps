package com.ranjanvipul.relayguard.data.repository

import com.ranjanvipul.relayguard.data.local.EntityMappers
import com.ranjanvipul.relayguard.data.local.FilterDao
import com.ranjanvipul.relayguard.data.local.MessageLogDao
import com.ranjanvipul.relayguard.data.local.RelayAttemptEntity
import com.ranjanvipul.relayguard.domain.model.ForwardFilter
import com.ranjanvipul.relayguard.domain.model.MessageEvent
import com.ranjanvipul.relayguard.domain.model.RelayAttempt
import com.ranjanvipul.relayguard.domain.model.RenderedRelay
import com.ranjanvipul.relayguard.domain.repository.FilterRepository
import com.ranjanvipul.relayguard.domain.repository.MessageLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomFilterRepository(
    private val dao: FilterDao,
    private val mappers: EntityMappers
) : FilterRepository {
    override fun observeFilters(): Flow<List<ForwardFilter>> = dao.observeAll().map { rows ->
        rows.map(mappers::entityToFilter)
    }

    override suspend fun saveFilter(filter: ForwardFilter) {
        dao.upsert(mappers.filterToEntity(filter))
    }

    override suspend fun setFilterEnabled(filterId: String, enabled: Boolean) {
        dao.setEnabled(filterId, enabled)
    }

    override suspend fun deleteFilter(filterId: String) {
        dao.delete(filterId)
    }
}

class RoomMessageLogRepository(
    private val dao: MessageLogDao,
    private val mappers: EntityMappers,
    private val clock: () -> Long = System::currentTimeMillis
) : MessageLogRepository {
    override fun observeRecentEvents(limit: Int): Flow<List<MessageEvent>> =
        dao.observeRecent(limit).map { rows -> rows.map(mappers::entityToEvent) }

    override fun observeRecentRelays(limit: Int): Flow<List<RelayAttempt>> =
        dao.observeRecentRelays(limit).map { rows -> rows.map(mappers::entityToRelayAttempt) }

    override suspend fun recordEvent(event: MessageEvent) {
        dao.insertEvent(mappers.eventToEntity(event))
    }

    override suspend fun recordRelay(relay: RenderedRelay, success: Boolean, detail: String?) {
        dao.insertAttempt(
            RelayAttemptEntity(
                filterId = relay.filterId,
                recipientId = relay.recipient.id,
                recipientKind = relay.recipient.kind.name,
                bodyPreview = relay.body.take(160),
                success = success,
                detail = detail,
                createdAtEpochMillis = clock()
            )
        )
    }
}
