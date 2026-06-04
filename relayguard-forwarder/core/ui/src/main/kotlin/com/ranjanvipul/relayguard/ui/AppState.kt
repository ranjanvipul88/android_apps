package com.ranjanvipul.relayguard.ui

import com.ranjanvipul.relayguard.domain.model.ForwardFilter
import com.ranjanvipul.relayguard.domain.model.MessageEvent
import com.ranjanvipul.relayguard.domain.model.PermissionExplanation
import com.ranjanvipul.relayguard.domain.model.RelayAttempt

data class RelayGuardUiState(
    val filters: List<ForwardFilter> = emptyList(),
    val history: List<MessageEvent> = emptyList(),
    val relayAttempts: List<RelayAttempt> = emptyList(),
    val permissions: List<PermissionExplanation> = emptyList(),
    val isProcessing: Boolean = false,
    val status: String = "Ready"
)
