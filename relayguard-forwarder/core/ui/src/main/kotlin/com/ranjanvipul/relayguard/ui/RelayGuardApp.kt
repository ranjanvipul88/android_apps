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
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ranjanvipul.relayguard.domain.model.ForwardFilter
import com.ranjanvipul.relayguard.domain.model.MessageEvent

private enum class Tab { Filters, History, Privacy, Settings }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelayGuardApp(
    state: RelayGuardUiState,
    onToggleFilter: (String, Boolean) -> Unit,
    onCreateSampleFilter: () -> Unit
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
                FloatingActionButton(onClick = onCreateSampleFilter) {
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
                Tab.Filters -> FilterScreen(state.filters, onToggleFilter, Modifier.padding(padding))
                Tab.History -> HistoryScreen(state.history, Modifier.padding(padding))
                Tab.Privacy -> PrivacyScreen(state, Modifier.padding(padding))
                Tab.Settings -> SettingsScreen(Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun FilterScreen(filters: List<ForwardFilter>, onToggle: (String, Boolean) -> Unit, modifier: Modifier) {
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
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(history: List<MessageEvent>, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (history.isEmpty()) {
            item { EmptyCard("No message activity", "Matched events and delivery results will appear here.") }
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
private fun SettingsScreen(modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { EmptyCard("Delivery settings", "Configure HTTPS, SMTP, chat webhooks, local backup, and app lock in the production settings flow.") }
        item { EmptyCard("Security defaults", "Cleartext traffic is disabled outside debug/local development. Secrets are stored with Android Keystore.") }
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
