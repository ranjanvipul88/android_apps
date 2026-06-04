package com.ranjanvipul.relayguard.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ranjanvipul.relayguard.domain.model.ForwardFilter
import com.ranjanvipul.relayguard.domain.model.MessageEvent
import com.ranjanvipul.relayguard.domain.model.MessageKind
import com.ranjanvipul.relayguard.domain.model.RelayAttempt

private enum class Tab { Filters, History, Privacy, Settings }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelayGuardApp(
    state: RelayGuardUiState,
    onToggleFilter: (String, Boolean) -> Unit,
    onDeleteFilter: (String) -> Unit,
    onCreateSmsForward: (String, Set<MessageKind>) -> Unit,
    onCreateEmailForward: (String, String, String, String, String, String, Boolean, Set<MessageKind>) -> Unit,
    onCreateTelegramForward: (String, String, Set<MessageKind>) -> Unit,
    onCreateSlackForward: (String, Set<MessageKind>) -> Unit,
    onCreateWebhookForward: (String, Set<MessageKind>) -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit
) {
    var tab by remember { mutableStateOf(Tab.Filters) }
    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("RelayGuard", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(state.status, style = MaterialTheme.typography.labelMedium)
                    }
                }
            )
        },
        floatingActionButton = {
            if (tab == Tab.Filters) {
                FloatingActionButton(onClick = { tab = Tab.Settings }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Create forwarding rule")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.Filters,
                    onClick = { tab = Tab.Filters },
                    icon = { Icon(Icons.AutoMirrored.Outlined.Rule, contentDescription = null) },
                    label = { Text("Rules") }
                )
                NavigationBarItem(
                    selected = tab == Tab.History,
                    onClick = { tab = Tab.History },
                    icon = { Icon(Icons.Outlined.History, contentDescription = null) },
                    label = { Text("History") }
                )
                NavigationBarItem(
                    selected = tab == Tab.Privacy,
                    onClick = { tab = Tab.Privacy },
                    icon = { Icon(Icons.Outlined.Security, contentDescription = null) },
                    label = { Text("Privacy") }
                )
                NavigationBarItem(
                    selected = tab == Tab.Settings,
                    onClick = { tab = Tab.Settings },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Crossfade(targetState = tab, label = "screen") { current ->
            when (current) {
                Tab.Filters -> FilterScreen(state.filters, onToggleFilter, onDeleteFilter, Modifier.padding(padding))
                Tab.History -> HistoryScreen(state.history, state.relayAttempts, Modifier.padding(padding))
                Tab.Privacy -> PrivacyScreen(state, Modifier.padding(padding))
                Tab.Settings -> SettingsScreen(
                    onCreateSmsForward = onCreateSmsForward,
                    onCreateEmailForward = onCreateEmailForward,
                    onCreateTelegramForward = onCreateTelegramForward,
                    onCreateSlackForward = onCreateSlackForward,
                    onCreateWebhookForward = onCreateWebhookForward,
                    onExportBackup = onExportBackup,
                    onRestoreBackup = onRestoreBackup,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun FilterScreen(
    filters: List<ForwardFilter>,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (filters.isEmpty()) {
            item {
                EmptyCard("No forwarding rules yet", "Use the add button to create a local, user-controlled relay rule.")
            }
        }
        items(filters, key = { it.id }) { filter ->
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(filter.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("${filter.messageKinds.size} event type(s), ${filter.recipients.size} destination(s)")
                        }
                        Switch(
                            checked = filter.enabled,
                            onCheckedChange = { onToggle(filter.id, it) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        filter.messageKinds.take(3).forEach { AssistChip(onClick = {}, label = { Text(it.name) }) }
                    }
                    Button(onClick = { onDelete(filter.id) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(history: List<MessageEvent>, attempts: List<RelayAttempt>, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (history.isEmpty() && attempts.isEmpty()) {
            item { EmptyCard("No message activity", "Matched events and delivery results will appear here.") }
        }
        if (attempts.isNotEmpty()) {
            item { Text("Delivery attempts", style = MaterialTheme.typography.titleMedium) }
        }
        items(attempts, key = { it.id }) { attempt ->
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (attempt.success) "Delivered to ${attempt.recipientKind.name}" else "Failed: ${attempt.recipientKind.name}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(attempt.detail ?: "No error reported", style = MaterialTheme.typography.bodySmall)
                    Text(attempt.bodyPreview, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (history.isNotEmpty()) {
            item { Text("Received messages", style = MaterialTheme.typography.titleMedium) }
        }
        items(history, key = { it.id }) { event ->
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(event.kind.name, style = MaterialTheme.typography.labelLarge)
                    Text(event.sender, style = MaterialTheme.typography.titleSmall)
                    Text(event.body, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun PrivacyScreen(state: RelayGuardUiState, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.permissions, key = { it.permission }) { permission ->
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(permission.permission, style = MaterialTheme.typography.titleMedium)
                    Text(permission.userFacingReason)
                    Text(permission.developerNote, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    onCreateSmsForward: (String, Set<MessageKind>) -> Unit,
    onCreateEmailForward: (String, String, String, String, String, String, Boolean, Set<MessageKind>) -> Unit,
    onCreateTelegramForward: (String, String, Set<MessageKind>) -> Unit,
    onCreateSlackForward: (String, Set<MessageKind>) -> Unit,
    onCreateWebhookForward: (String, Set<MessageKind>) -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    modifier: Modifier
) {
    var smsPhone by rememberSaveable { mutableStateOf("") }
    var emailTo by rememberSaveable { mutableStateOf("") }
    var smtpHost by rememberSaveable { mutableStateOf("") }
    var smtpPort by rememberSaveable { mutableStateOf("587") }
    var smtpUser by rememberSaveable { mutableStateOf("") }
    var smtpPassword by rememberSaveable { mutableStateOf("") }
    var smtpFrom by rememberSaveable { mutableStateOf("") }
    var smtpSsl by rememberSaveable { mutableStateOf(false) }
    var telegramToken by rememberSaveable { mutableStateOf("") }
    var telegramChatId by rememberSaveable { mutableStateOf("") }
    var slackWebhook by rememberSaveable { mutableStateOf("") }
    var genericWebhook by rememberSaveable { mutableStateOf("") }
    var includeSms by rememberSaveable { mutableStateOf(true) }
    var includeMms by rememberSaveable { mutableStateOf(false) }
    var includeRcs by rememberSaveable { mutableStateOf(false) }
    var includeOutgoing by rememberSaveable { mutableStateOf(false) }
    var includeNotifications by rememberSaveable { mutableStateOf(false) }
    val selectedKinds = buildSet {
        if (includeSms) add(MessageKind.IncomingSms)
        if (includeMms) add(MessageKind.IncomingMmsNotice)
        if (includeRcs) add(MessageKind.IncomingRcs)
        if (includeOutgoing) add(MessageKind.OutgoingSms)
        if (includeNotifications) add(MessageKind.AppNotification)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Event types", style = MaterialTheme.typography.titleMedium)
                    EventKindRow("Incoming SMS", includeSms) { includeSms = it }
                    EventKindRow("MMS notices", includeMms) { includeMms = it }
                    EventKindRow("RCS broadcasts", includeRcs) { includeRcs = it }
                    EventKindRow("Outgoing SMS", includeOutgoing) { includeOutgoing = it }
                    EventKindRow("App notifications", includeNotifications) { includeNotifications = it }
                }
            }
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Forward to another phone", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = smsPhone,
                        onValueChange = { smsPhone = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Destination phone number") },
                        singleLine = true
                    )
                    Button(onClick = { onCreateSmsForward(smsPhone, selectedKinds) }, enabled = smsPhone.isNotBlank() && selectedKinds.isNotEmpty()) {
                        Text("Enable SMS forwarding")
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Forward to email", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = emailTo,
                        onValueChange = { emailTo = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Recipient email") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = smtpHost,
                        onValueChange = { smtpHost = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("SMTP host") },
                        singleLine = true
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = smtpPort,
                            onValueChange = { smtpPort = it.filter(Char::isDigit) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Port") },
                            singleLine = true
                        )
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Checkbox(checked = smtpSsl, onCheckedChange = { smtpSsl = it })
                            Text("SSL/TLS", modifier = Modifier.padding(top = 14.dp))
                        }
                    }
                    OutlinedTextField(
                        value = smtpUser,
                        onValueChange = { smtpUser = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("SMTP username") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = smtpPassword,
                        onValueChange = { smtpPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("SMTP password or app password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        value = smtpFrom,
                        onValueChange = { smtpFrom = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("From address") },
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            onCreateEmailForward(emailTo, smtpHost, smtpPort, smtpUser, smtpPassword, smtpFrom, smtpSsl, selectedKinds)
                        },
                        enabled = emailTo.isNotBlank() && smtpHost.isNotBlank() && selectedKinds.isNotEmpty()
                    ) {
                        Text("Enable email forwarding")
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Forward to Telegram contact", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = telegramToken,
                        onValueChange = { telegramToken = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Bot token") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        value = telegramChatId,
                        onValueChange = { telegramChatId = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Contact chat id") },
                        singleLine = true
                    )
                    Button(
                        onClick = { onCreateTelegramForward(telegramToken, telegramChatId, selectedKinds) },
                        enabled = telegramToken.isNotBlank() && telegramChatId.isNotBlank() && selectedKinds.isNotEmpty()
                    ) {
                        Text("Enable Telegram contact forwarding")
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Forward to Slack", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = slackWebhook,
                        onValueChange = { slackWebhook = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Incoming webhook URL") },
                        singleLine = true
                    )
                    Button(
                        onClick = { onCreateSlackForward(slackWebhook, selectedKinds) },
                        enabled = slackWebhook.startsWith("https://") && selectedKinds.isNotEmpty()
                    ) {
                        Text("Enable Slack forwarding")
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Forward to HTTPS webhook", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = genericWebhook,
                        onValueChange = { genericWebhook = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Webhook URL") },
                        singleLine = true
                    )
                    Button(
                        onClick = { onCreateWebhookForward(genericWebhook, selectedKinds) },
                        enabled = genericWebhook.startsWith("https://") && selectedKinds.isNotEmpty()
                    ) {
                        Text("Enable webhook forwarding")
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Backup and restore", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = onExportBackup) {
                        Text("Save local backup")
                    }
                    Button(onClick = onRestoreBackup) {
                        Text("Restore local backup")
                    }
                }
            }
        }
        item { EmptyCard("Security defaults", "Cleartext traffic is disabled outside debug/local development. Secrets are stored with Android Keystore.") }
    }
}

@Composable
private fun EventKindRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, modifier = Modifier.padding(top = 14.dp))
    }
}

@Composable
private fun EmptyCard(title: String, body: String) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
