package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleBottomSheet(
    title: String,
    initialLabelText: String,
    initialContactPattern: String,
    initialAllowBankingOtp: Boolean,
    initialSimSlot: Int,
    initialMaxRetries: Int,
    initialIsSmsEnabled: Boolean,
    initialForwardNumber: String,
    initialIsTelegramEnabled: Boolean,
    initialTelegramBotToken: String,
    initialTelegramChatId: String,
    initialIsEmailEnabled: Boolean,
    initialSmtpSenderEmail: String,
    initialSmtpAppPassword: String,
    initialTargetEmail: String,
    onDismiss: () -> Unit,
    onSubmit: (
        pattern: String, forward: String, label: String, allowBanking: Boolean,
        simSlot: Int, retries: Int, isTelegram: Boolean, botToken: String,
        chatId: String, isEmail: Boolean, smtpSender: String, smtpPass: String, targetEmail: String
    ) -> Unit
) {
    var labelText by remember { mutableStateOf(initialLabelText) }
    var contactPattern by remember { mutableStateOf(initialContactPattern) }
    var allowBankingOtp by remember { mutableStateOf(initialAllowBankingOtp) }
    var simSlot by remember { mutableStateOf(initialSimSlot) }
    var maxRetries by remember { mutableStateOf(initialMaxRetries) }
    
    var isSmsEnabled by remember { mutableStateOf(initialIsSmsEnabled) }
    var forwardNumber by remember { mutableStateOf(initialForwardNumber) }

    var isTelegramEnabled by remember { mutableStateOf(initialIsTelegramEnabled) }
    var telegramBotToken by remember { mutableStateOf(initialTelegramBotToken) }
    var telegramChatId by remember { mutableStateOf(initialTelegramChatId) }

    var isEmailEnabled by remember { mutableStateOf(initialIsEmailEnabled) }
    var smtpSenderEmail by remember { mutableStateOf(initialSmtpSenderEmail) }
    var smtpAppPassword by remember { mutableStateOf(initialSmtpAppPassword) }
    var targetEmail by remember { mutableStateOf(initialTargetEmail) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Rules run in background automatically and evaluate message origins before forwarding.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            OutlinedTextField(
                value = labelText,
                onValueChange = { labelText = it },
                label = { Text("Rule name / label (e.g. Spouse)") },
                modifier = Modifier.fillMaxWidth().testTag("rule_label_input"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = contactPattern,
                onValueChange = { contactPattern = it },
                label = { Text("Sender criteria (All Contacts/Numbers or number)") },
                modifier = Modifier.fillMaxWidth().testTag("rule_pattern_input"),
                placeholder = { Text("e.g. All Contacts/Numbers or +15550000") },
                shape = RoundedCornerShape(12.dp)
            )

            // Warning and Toggle for Banking / OTP
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow Banking & OTP", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Enable dynamic bypass of safety guardrails.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = allowBankingOtp, onCheckedChange = { allowBankingOtp = it })
                }
                if (allowBankingOtp) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Warning: Bypassing these guardrails allows financial transaction notifications and OTPs to be forwarded. Only do this if you completely trust the recipient.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Advanced Options
            var expandedSim by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedSim,
                onExpandedChange = { expandedSim = !expandedSim }
            ) {
                val simText = when (simSlot) { 0 -> "SIM 1"; 1 -> "SIM 2"; else -> "Any SIM" }
                OutlinedTextField(
                    value = simText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Target SIM Slot") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSim) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = expandedSim, onDismissRequest = { expandedSim = false }) {
                    DropdownMenuItem(text = { Text("Any SIM") }, onClick = { simSlot = -1; expandedSim = false })
                    DropdownMenuItem(text = { Text("SIM 1") }, onClick = { simSlot = 0; expandedSim = false })
                    DropdownMenuItem(text = { Text("SIM 2") }, onClick = { simSlot = 1; expandedSim = false })
                }
            }

            OutlinedTextField(
                value = if (maxRetries == 0) "" else maxRetries.toString(),
                onValueChange = { maxRetries = it.toIntOrNull() ?: 0 },
                label = { Text("Max Retries (Optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Forwarding Destinations", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            // SMS Forwarding
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isSmsEnabled, onCheckedChange = { isSmsEnabled = it; if (!it) forwardNumber = "" })
                        Text("SMS Forwarding", fontWeight = FontWeight.Bold)
                    }
                    if (isSmsEnabled) {
                        OutlinedTextField(
                            value = forwardNumber,
                            onValueChange = { forwardNumber = it },
                            label = { Text("Forward to number (recipient)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth().testTag("rule_destination_input"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Telegram Forwarding
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isTelegramEnabled, onCheckedChange = { isTelegramEnabled = it })
                        Text("Telegram Forwarding", fontWeight = FontWeight.Bold)
                    }
                    if (isTelegramEnabled) {
                        OutlinedTextField(
                            value = telegramBotToken,
                            onValueChange = { telegramBotToken = it },
                            label = { Text("Bot Token") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = telegramChatId,
                            onValueChange = { telegramChatId = it },
                            label = { Text("Chat ID") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Email Configuration
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isEmailEnabled, onCheckedChange = { isEmailEnabled = it })
                        Text("Email Fallback (SMTP)", fontWeight = FontWeight.Bold)
                    }
                    if (isEmailEnabled) {
                        OutlinedTextField(
                            value = smtpSenderEmail,
                            onValueChange = { smtpSenderEmail = it },
                            label = { Text("SMTP Sender Email") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = smtpAppPassword,
                            onValueChange = { smtpAppPassword = it },
                            label = { Text("App Password") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = targetEmail,
                            onValueChange = { targetEmail = it },
                            label = { Text("Target Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val fwd = if (isSmsEnabled) forwardNumber.trim() else ""
                    val pat = contactPattern.trim().ifEmpty { "All Contacts/Numbers" }
                    val lbl = labelText.trim().ifEmpty { "Filter: $pat" }
                    if (fwd.isNotEmpty() || isTelegramEnabled || isEmailEnabled) {
                        onSubmit(pat, fwd, lbl, allowBankingOtp, simSlot, maxRetries, isTelegramEnabled, telegramBotToken.trim(), telegramChatId.trim(), isEmailEnabled, smtpSenderEmail.trim(), smtpAppPassword.trim(), targetEmail.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("submit_rule_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (title.contains("Edit")) "Save Changes" else "Establish Rule", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
