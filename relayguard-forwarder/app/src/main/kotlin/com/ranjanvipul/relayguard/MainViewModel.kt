package com.ranjanvipul.relayguard

import com.google.gson.Gson
import com.ranjanvipul.relayguard.data.security.SecretStore
import com.ranjanvipul.relayguard.data.transport.SmtpEmailConfig
import com.ranjanvipul.relayguard.data.transport.SmtpSecurity
import com.ranjanvipul.relayguard.data.transport.WhatsAppBusinessConfig
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
    messageLogRepository: MessageLogRepository,
    private val secretStore: SecretStore
) : ViewModel() {
    val uiState = combine(
        filterRepository.observeFilters(),
        messageLogRepository.observeRecentEvents(50),
        messageLogRepository.observeRecentRelays(50)
    ) { filters, history, attempts ->
        RelayGuardUiState(
            filters = filters,
            history = history,
            relayAttempts = attempts,
            permissions = PermissionCatalog.required + PermissionCatalog.optional,
            status = if (filters.any { it.enabled }) "Rules active" else "Paused"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RelayGuardUiState())

    fun setFilterEnabled(filterId: String, enabled: Boolean) {
        viewModelScope.launch {
            filterRepository.setFilterEnabled(filterId, enabled)
        }
    }

    fun createSmsForwardRule(phoneNumber: String) {
        val cleanPhoneNumber = phoneNumber.trim()
        if (cleanPhoneNumber.isBlank()) return
        viewModelScope.launch {
            filterRepository.saveFilter(
                newForwardAllFilter(
                    name = "Forward SMS to $cleanPhoneNumber",
                    recipient = Recipient(
                        id = UUID.randomUUID().toString(),
                        kind = RecipientKind.SmsNumber,
                        label = "SMS $cleanPhoneNumber",
                        address = cleanPhoneNumber
                    )
                )
            )
        }
    }

    fun createEmailForwardRule(
        toAddress: String,
        host: String,
        port: String,
        username: String,
        password: String,
        fromAddress: String,
        useSsl: Boolean
    ) {
        val cleanTo = toAddress.trim()
        val cleanHost = host.trim()
        val cleanFrom = fromAddress.trim().ifBlank { username.trim() }
        val cleanUsername = username.trim()
        val cleanPassword = password.trim()
        val parsedPort = port.trim().toIntOrNull() ?: if (useSsl) 465 else 587
        if (cleanTo.isBlank() || cleanHost.isBlank() || cleanFrom.isBlank()) return
        val secretAlias = "smtp-password-${UUID.randomUUID()}"
        val config = SmtpEmailConfig(
            host = cleanHost,
            port = parsedPort,
            security = if (useSsl) SmtpSecurity.SslTls else SmtpSecurity.StartTls,
            username = cleanUsername,
            fromAddress = cleanFrom,
            toAddress = cleanTo
        )
        viewModelScope.launch {
            if (cleanPassword.isNotBlank()) secretStore.put(secretAlias, cleanPassword)
            filterRepository.saveFilter(
                newForwardAllFilter(
                    name = "Forward SMS to $cleanTo",
                    recipient = Recipient(
                        id = UUID.randomUUID().toString(),
                        kind = RecipientKind.Email,
                        label = "Email $cleanTo",
                        address = Gson().toJson(config),
                        secretAlias = secretAlias
                    )
                )
            )
        }
    }

    fun createWhatsAppBusinessForwardRule(endpoint: String, toPhoneNumber: String, token: String) {
        val cleanEndpoint = endpoint.trim()
        val cleanPhone = toPhoneNumber.trim()
        val cleanToken = token.trim()
        if (!cleanEndpoint.startsWith("https://") || cleanPhone.isBlank() || cleanToken.isBlank()) return
        val secretAlias = "whatsapp-token-${UUID.randomUUID()}"
        val config = WhatsAppBusinessConfig(
            messagesEndpoint = cleanEndpoint,
            toPhoneNumber = cleanPhone
        )
        viewModelScope.launch {
            secretStore.put(secretAlias, cleanToken)
            filterRepository.saveFilter(
                newForwardAllFilter(
                    name = "Forward SMS to WhatsApp Business",
                    recipient = Recipient(
                        id = UUID.randomUUID().toString(),
                        kind = RecipientKind.WhatsAppBusiness,
                        label = "WhatsApp Business $cleanPhone",
                        address = Gson().toJson(config),
                        secretAlias = secretAlias
                    )
                )
            )
        }
    }

    fun createStarterFilter() = createSmsForwardRule("")

    private fun newForwardAllFilter(name: String, recipient: Recipient): ForwardFilter = ForwardFilter(
        id = UUID.randomUUID().toString(),
        name = name,
        enabled = true,
        messageKinds = setOf(MessageKind.IncomingSms),
        conditions = emptyList(),
        recipients = listOf(recipient),
        template = "SMS from {{sender}}\n\n{{message}}\n\nReceived: {{time}}"
    )
}
