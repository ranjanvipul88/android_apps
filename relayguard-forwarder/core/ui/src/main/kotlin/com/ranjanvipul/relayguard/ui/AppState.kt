package com.ranjanvipul.relayguard.ui

import com.ranjanvipul.relayguard.domain.model.ForwardFilter
import com.ranjanvipul.relayguard.domain.model.MessageEvent
import com.ranjanvipul.relayguard.domain.model.PermissionExplanation

data class RelayGuardUiState(
    val filters: List<ForwardFilter> = emptyList(),
    val history: List<MessageEvent> = emptyList(),
    val permissions: List<PermissionExplanation> = emptyList(),
    val isProcessing: Boolean = false,
    val status: String = "Ready"
)
