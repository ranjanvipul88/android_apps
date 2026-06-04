package com.ranjanvipul.relayguard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranjanvipul.relayguard.domain.model.ForwardFilter
import com.ranjanvipul.relayguard.domain.model.MessageKind
import com.ranjanvipul.relayguard.domain.model.Recipient
import com.ranjanvipul.relayguard.domain.model.RecipientKind
import com.ranjanvipul.relayguard.domain.repository.FilterRepository
import com.ranjanvipul.relayguard.domain.repository.MessageLogRepository
import com.ranjanvipul.relayguard.domain.usecase.PermissionCatalog
import com.ranjanvipul.relayguard.ui.RelayGuardUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val filterRepository: FilterRepository,
    messageLogRepository: MessageLogRepository
) : ViewModel() {
    val uiState = combine(
        filterRepository.observeFilters(),
        messageLogRepository.observeRecentEvents(50)
    ) { filters, history ->
        RelayGuardUiState(
            filters = filters,
            history = history,
            permissions = PermissionCatalog.required + PermissionCatalog.optional,
            status = if (filters.any { it.enabled }) "Rules active" else "Paused"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RelayGuardUiState())

    fun setFilterEnabled(filterId: String, enabled: Boolean) {
        viewModelScope.launch {
            filterRepository.setFilterEnabled(filterId, enabled)
        }
    }

    fun createStarterFilter() {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            filterRepository.saveFilter(
                ForwardFilter(
                    id = id,
                    name = "Starter HTTPS relay",
                    enabled = false,
                    messageKinds = setOf(MessageKind.IncomingSms),
                    conditions = emptyList(),
                    recipients = listOf(
                        Recipient(
                            id = UUID.randomUUID().toString(),
                            kind = RecipientKind.Webhook,
                            label = "Example endpoint",
                            address = "https://example.com/relay"
                        )
                    ),
                    template = "Sender {{sender}}\nMessage {{message}}\nReceived {{time}}"
                )
            )
        }
    }
}
