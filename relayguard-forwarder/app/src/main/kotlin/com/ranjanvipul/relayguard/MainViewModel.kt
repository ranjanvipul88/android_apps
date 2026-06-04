package com.ranjanvipul.relayguard

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ranjanvipul.relayguard.data.security.SecretStore
import com.ranjanvipul.relayguard.data.transport.SmtpEmailConfig
import com.ranjanvipul.relayguard.data.transport.SmtpSecurity
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
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val filterRepository: FilterRepository,
    messageLogRepository: MessageLogRepository,
    private val secretStore: SecretStore
) : ViewModel() {
    private val operationStatus = MutableStateFlow<String?>(null)

    val uiState = combine(
        filterRepository.observeFilters(),
        messageLogRepository.observeRecentEvents(50),
        messageLogRepository.observeRecentRelays(50),
        operationStatus
    ) { filters, history, attempts, statusOverride ->
        RelayGuardUiState(
            filters = filters,
            history = history,
            relayAttempts = attempts,
            permissions = PermissionCatalog.required + PermissionCatalog.optional,
            status = statusOverride ?: if (filters.any { it.enabled }) "Rules active" else "Paused"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RelayGuardUiState())

    fun setFilterEnabled(filterId: String, enabled: Boolean) {
        viewModelScope.launch {
            filterRepository.setFilterEnabled(filterId, enabled)
        }
    }

    fun deleteFilter(filterId: String) {
        viewModelScope.launch {
            filterRepository.deleteFilter(filterId)
        }
    }

    fun createSmsForwardRule(phoneNumber: String, messageKinds: Set<MessageKind>) {
        val cleanPhoneNumber = phoneNumber.trim()
        if (cleanPhoneNumber.isBlank()) return
        viewModelScope.launch {
            filterRepository.saveFilter(
                newForwardAllFilter(
                    name = "Forward SMS to $cleanPhoneNumber",
                    messageKinds = messageKinds.ifEmpty { setOf(MessageKind.IncomingSms) },
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
        useSsl: Boolean,
        messageKinds: Set<MessageKind>
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
                    messageKinds = messageKinds.ifEmpty { setOf(MessageKind.IncomingSms) },
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

    fun createTelegramForwardRule(botToken: String, chatId: String, messageKinds: Set<MessageKind>) {
        val cleanToken = botToken.trim()
        val cleanChatId = chatId.trim()
        if (cleanToken.isBlank() || cleanChatId.isBlank()) return
        val secretAlias = "telegram-token-${UUID.randomUUID()}"
        viewModelScope.launch {
            secretStore.put(secretAlias, cleanToken)
            filterRepository.saveFilter(
                newForwardAllFilter(
                    name = "Forward SMS to Telegram contact",
                    messageKinds = messageKinds.ifEmpty { setOf(MessageKind.IncomingSms) },
                    recipient = Recipient(
                        id = UUID.randomUUID().toString(),
                        kind = RecipientKind.Telegram,
                        label = "Telegram contact $cleanChatId",
                        address = cleanChatId,
                        secretAlias = secretAlias
                    )
                )
            )
        }
    }

    fun createSlackForwardRule(webhookUrl: String, messageKinds: Set<MessageKind>) {
        val cleanUrl = webhookUrl.trim()
        if (!cleanUrl.startsWith("https://")) return
        viewModelScope.launch {
            filterRepository.saveFilter(
                newForwardAllFilter(
                    name = "Forward SMS to Slack",
                    messageKinds = messageKinds.ifEmpty { setOf(MessageKind.IncomingSms) },
                    recipient = Recipient(
                        id = UUID.randomUUID().toString(),
                        kind = RecipientKind.Slack,
                        label = "Slack webhook",
                        address = cleanUrl
                    )
                )
            )
        }
    }

    fun createWebhookForwardRule(webhookUrl: String, messageKinds: Set<MessageKind>) {
        val cleanUrl = webhookUrl.trim()
        if (!cleanUrl.startsWith("https://")) return
        viewModelScope.launch {
            filterRepository.saveFilter(
                newForwardAllFilter(
                    name = "Forward SMS to webhook",
                    messageKinds = messageKinds.ifEmpty { setOf(MessageKind.IncomingSms) },
                    recipient = Recipient(
                        id = UUID.randomUUID().toString(),
                        kind = RecipientKind.Webhook,
                        label = "HTTPS webhook",
                        address = cleanUrl
                    )
                )
            )
        }
    }

    fun exportBackup() {
        viewModelScope.launch {
            runCatching {
                val filters = filterRepository.observeFilters().first()
                val secrets = filters
                    .flatMap { filter -> filter.recipients }
                    .mapNotNull { recipient ->
                        val alias = recipient.secretAlias ?: return@mapNotNull null
                        alias to secretStore.get(alias).orEmpty()
                    }
                    .filter { it.second.isNotBlank() }
                    .toMap()
                val file = backupFile()
                file.writeText(Gson().toJson(BackupPayload(filters, secrets)), Charsets.UTF_8)
                operationStatus.value = "Backup saved: ${file.absolutePath}"
            }.onFailure { error ->
                operationStatus.value = "Backup failed: ${error.message}"
            }
        }
    }

    fun restoreBackup() {
        viewModelScope.launch {
            runCatching {
                val file = backupFile()
                require(file.exists()) { "No local backup file found." }
                val payload = parseBackup(file.readText(Charsets.UTF_8))
                payload.secrets.forEach { (alias, value) -> secretStore.put(alias, value) }
                payload.filters.forEach { filterRepository.saveFilter(it) }
                operationStatus.value = "Restored ${payload.filters.size} rule(s)."
            }.onFailure { error ->
                operationStatus.value = "Restore failed: ${error.message}"
            }
        }
    }

    fun createStarterFilter() = createSmsForwardRule("", setOf(MessageKind.IncomingSms))

    private fun newForwardAllFilter(
        name: String,
        messageKinds: Set<MessageKind>,
        recipient: Recipient
    ): ForwardFilter = ForwardFilter(
        id = UUID.randomUUID().toString(),
        name = name,
        enabled = true,
        messageKinds = messageKinds,
        conditions = emptyList(),
        recipients = listOf(recipient),
        template = "SMS from {{sender}}\n\n{{message}}\n\nReceived: {{time}}"
    )

    private fun backupFile(): File = File(context.filesDir, "relayguard-rules-backup.json")

    private fun parseBackup(json: String): BackupPayload {
        val gson = Gson()
        return runCatching {
            gson.fromJson(json, BackupPayload::class.java)
        }.getOrElse {
            val listType = object : TypeToken<List<ForwardFilter>>() {}.type
            BackupPayload(gson.fromJson(json, listType), emptyMap())
        }
    }

    private data class BackupPayload(
        val filters: List<ForwardFilter>,
        val secrets: Map<String, String>
    )
}
