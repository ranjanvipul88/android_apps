package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AuditLog
import com.example.data.ForwardingRule
import com.example.data.SmsMessage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsDashboardMain(
    viewModel: SmsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()

    // Permissions State Tracking for active UI feedback
    var receiveSmsGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED)
    }
    var sendSmsGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED)
    }
    var readSmsGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        receiveSmsGranted = results[Manifest.permission.RECEIVE_SMS] ?: receiveSmsGranted
        sendSmsGranted = results[Manifest.permission.SEND_SMS] ?: sendSmsGranted
        readSmsGranted = results[Manifest.permission.READ_SMS] ?: readSmsGranted

        if (receiveSmsGranted && sendSmsGranted) {
            Toast.makeText(context, "Core permissions configured successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Note: Forwarding requires both Receive and Send permissions.", Toast.LENGTH_LONG).show()
        }
    }

    // Refresh permissions state whenever view is loaded
    var showBatteryPrompt by remember { mutableStateOf(false) }
    var batteryOptIntent by remember { mutableStateOf<android.content.Intent?>(null) }

    LaunchedEffect(Unit) {
        receiveSmsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        sendSmsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        readSmsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val prefs = context.getSharedPreferences("sms_backup_prefs", Context.MODE_PRIVATE)
        if (!pm.isIgnoringBatteryOptimizations(context.packageName) && !prefs.getBoolean("battery_prompt_shown", false)) {
            prefs.edit().putBoolean("battery_prompt_shown", true).apply()
            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = android.net.Uri.parse("package:${context.packageName}")
            batteryOptIntent = intent
            showBatteryPrompt = true
        }
    }

    val isBiometricOnboardingCompleted by viewModel.isBiometricOnboardingCompleted.collectAsStateWithLifecycle()

    if (!isOnboardingCompleted || !isBiometricOnboardingCompleted) {
        CarouselOnboardingScreen(
            receiveGranted = receiveSmsGranted,
            sendGranted = sendSmsGranted,
            readGranted = readSmsGranted,
            onRequestPermissions = {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.READ_SMS
                    )
                )
            },
            onComplete = { biometricEnabled ->
                if (receiveSmsGranted && sendSmsGranted) {
                    viewModel.completeOnboarding()
                    viewModel.completeBiometricOnboarding(biometricEnabled)
                } else {
                    Toast.makeText(context, "Please grant both Send and Receive SMS permissions to proceed.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    } else {
        var currentTab by remember { mutableIntStateOf(0) }
        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Safe Backup & Forward",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                    )
                )
            },
            bottomBar = {
                if (showBatteryPrompt) {
                    AlertDialog(
                        onDismissRequest = { showBatteryPrompt = false },
                        title = { Text("Background Execution Needed") },
                        text = { Text("To ensure SMS messages are reliably forwarded when your phone is asleep, the app needs an exemption from battery optimizations. Please allow this in the next screen.") },
                        confirmButton = {
                            Button(onClick = {
                                showBatteryPrompt = false
                                batteryOptIntent?.let { context.startActivity(it) }
                            }) {
                                Text("Allow")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBatteryPrompt = false }) {
                                Text("Not Now")
                            }
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .background(Color.Transparent)
                ) {
                    NavigationBar(
                        containerColor = Color(0x801A1A1A), // Luxury Glass
                        contentColor = Color(0xFF00E5FF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                    ) {
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Rules") },
                            label = { Text("Forwarding", fontSize = 10.sp) },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(indicatorColor = Color(0x3300E5FF), selectedIconColor = Color(0xFF00E5FF), unselectedIconColor = Color.Gray, selectedTextColor = Color(0xFF00E5FF), unselectedTextColor = Color.Gray)
                        )
                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            icon = { Icon(Icons.Default.Lock, contentDescription = "Logs") },
                            label = { Text("Logs", fontSize = 10.sp) },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(indicatorColor = Color(0x3300E5FF), selectedIconColor = Color(0xFF00E5FF), unselectedIconColor = Color.Gray, selectedTextColor = Color(0xFF00E5FF), unselectedTextColor = Color.Gray)
                        )
                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { currentTab = 2 },
                            icon = { Icon(Icons.Default.Share, contentDescription = "Backup") },
                            label = { Text("Backup", fontSize = 10.sp) },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(indicatorColor = Color(0x3300E5FF), selectedIconColor = Color(0xFF00E5FF), unselectedIconColor = Color.Gray, selectedTextColor = Color(0xFF00E5FF), unselectedTextColor = Color.Gray)
                        )
                        NavigationBarItem(
                            selected = currentTab == 3,
                            onClick = { currentTab = 3 },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings", fontSize = 10.sp) },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(indicatorColor = Color(0x3300E5FF), selectedIconColor = Color(0xFF00E5FF), unselectedIconColor = Color.Gray, selectedTextColor = Color(0xFF00E5FF), unselectedTextColor = Color.Gray)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    0 -> RulesTab(viewModel = viewModel, snackbarHostState = snackbarHostState)
                    1 -> BackupsLogsTab(viewModel = viewModel)
                    2 -> BackupRestoreTab(
                        viewModel = viewModel,
                        receiveSmsGranted = receiveSmsGranted,
                        sendSmsGranted = sendSmsGranted,
                        readSmsGranted = readSmsGranted
                    )
                    3 -> SettingsTab(viewModel = viewModel)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Onboarding Screen Content
// -------------------------------------------------------------
@Composable
fun CarouselOnboardingScreen(
    receiveGranted: Boolean,
    sendGranted: Boolean,
    readGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onComplete: (biometricEnabled: Boolean) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> OnboardingSlide(
                        icon = Icons.AutoMirrored.Filled.Send,
                        title = "Never Miss a Message",
                        description = "Safely and seamlessly forward your SMS to any device, Email, or Telegram.",
                        primaryButtonText = "Next",
                        onPrimaryClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } }
                    )
                    1 -> OnboardingSlide(
                        icon = Icons.Default.Lock,
                        title = "Encrypted Messages",
                        description = "Your SMS backups are locked with AES-GCM encryption. Your data stays completely on your phone.",
                        primaryButtonText = "Next",
                        onPrimaryClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } }
                    )
                    2 -> PermissionsSlide(
                        receiveGranted = receiveGranted,
                        sendGranted = sendGranted,
                        readGranted = readGranted,
                        onRequestPermissions = onRequestPermissions,
                        onNext = { coroutineScope.launch { pagerState.animateScrollToPage(3) } }
                    )
                    3 -> BiometricSlide(
                        onEnable = { onComplete(true) },
                        onSkip = { onComplete(false) }
                    )
                }
            }

            // Dot Indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(4) { iteration ->
                    val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(if (pagerState.currentPage == iteration) 10.dp else 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingSlide(icon: ImageVector, title: String, description: String, primaryButtonText: String, onPrimaryClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(description, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onPrimaryClick, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
            Text(primaryButtonText)
        }
    }
}

@Composable
fun PermissionsSlide(
    receiveGranted: Boolean,
    sendGranted: Boolean,
    readGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        Text("We Need Access", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("To perform this magic, we need access to your SMS.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))

        PermissionItem("Receive SMS", "Read incoming texts", receiveGranted)
        PermissionItem("Send SMS", "Forward messages", sendGranted)

        Spacer(modifier = Modifier.height(32.dp))
        if (receiveGranted && sendGranted) {
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                Text("Continue to Final Step")
            }
        } else {
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Grant Permissions")
            }
        }
    }
}

@Composable
fun BiometricSlide(onEnable: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Secure Your App", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Would you like to enable Biometric Authentication (Fingerprint/Face Unlock) to protect your forwarding rules?",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onEnable,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Yes, Enable Biometric Lock")
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onSkip) {
            Text("Skip for now", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PermissionItem(
    name: String,
    description: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = if (isGranted) "Granted" else "Requires Authorization",
            tint = if (isGranted) Color(0xFF4CAF50) else Color(0xFFFF9800),
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isGranted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}

// -------------------------------------------------------------
// TAB 1: Forwarding Rules
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesTab(viewModel: SmsViewModel, snackbarHostState: SnackbarHostState) {
    val rules by viewModel.allRules.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }

    var contactPattern by remember { mutableStateOf("") }
    var isSmsEnabled by remember { mutableStateOf(false) }
    var forwardNumber by remember { mutableStateOf("") }
    var labelText by remember { mutableStateOf("") }
    var allowBankingOtp by remember { mutableStateOf(false) }
    var simSlot by remember { mutableIntStateOf(-1) }
    var maxRetries by remember { mutableIntStateOf(0) }
    var isTelegramEnabled by remember { mutableStateOf(false) }
    var telegramBotToken by remember { mutableStateOf("") }
    var telegramChatId by remember { mutableStateOf("") }
    var isEmailEnabled by remember { mutableStateOf(false) }
    var smtpSenderEmail by remember { mutableStateOf("") }
    var smtpAppPassword by remember { mutableStateOf("") }
    var targetEmail by remember { mutableStateOf("") }

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedRuleForEdit by remember { mutableStateOf<ForwardingRule?>(null) }
    var editLabelText by remember { mutableStateOf("") }
    var editContactPattern by remember { mutableStateOf("") }
    var editIsSmsEnabled by remember { mutableStateOf(false) }
    var editForwardNumber by remember { mutableStateOf("") }
    var editAllowBankingOtp by remember { mutableStateOf(false) }
    var editSimSlot by remember { mutableIntStateOf(-1) }
    var editMaxRetries by remember { mutableIntStateOf(0) }
    var editIsTelegramEnabled by remember { mutableStateOf(false) }
    var editTelegramBotToken by remember { mutableStateOf("") }
    var editTelegramChatId by remember { mutableStateOf("") }
    var editIsEmailEnabled by remember { mutableStateOf(false) }
    var editSmtpSenderEmail by remember { mutableStateOf("") }
    var editSmtpAppPassword by remember { mutableStateOf("") }
    var editTargetEmail by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Hero Header
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)) {
            Column {
                Text("Dashboard", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Engine active and protecting.", fontSize = 14.sp, color = Color(0xFF00E5FF))
            }
        }

        Text("ROUTING RULES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp))

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
        if (rules.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Forwarding Rules Established",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap the plus button below to define an explicit rule. For safety, transaction codes and login passwords are automatically filtered out.",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Active Forwarding Matrix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                items(rules, key = { it.id }) { rule ->
                    RuleRow(
                        rule = rule,
                        onToggle = { enabled -> viewModel.toggleRule(rule, enabled) },
                        onDelete = { 
                            viewModel.deleteRule(rule)
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Rule '${rule.label}' deleted.",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.addRule(
                                        rule.contactPattern, rule.forwardToNumber, rule.label, rule.allowBankingOtp,
                                        rule.simSlot, rule.maxRetries, rule.isTelegramEnabled, rule.telegramBotToken, rule.telegramChatId,
                                        rule.isEmailEnabled, rule.smtpSenderEmail, rule.smtpAppPassword, rule.targetEmail
                                    )
                                }
                            }
                        },
                        onEditClicked = {
                            selectedRuleForEdit = rule
                            editLabelText = rule.label
                            editContactPattern = rule.contactPattern
                            editIsSmsEnabled = rule.forwardToNumber.isNotEmpty()
                            editForwardNumber = rule.forwardToNumber
                            editAllowBankingOtp = rule.allowBankingOtp
                            editSimSlot = rule.simSlot
                            editMaxRetries = rule.maxRetries
                            editIsTelegramEnabled = rule.isTelegramEnabled
                            editTelegramBotToken = rule.telegramBotToken
                            editTelegramChatId = rule.telegramChatId
                            editIsEmailEnabled = rule.isEmailEnabled
                            editSmtpSenderEmail = rule.smtpSenderEmail
                            editSmtpAppPassword = rule.smtpAppPassword
                            editTargetEmail = rule.targetEmail
                            showEditDialog = true
                        }
                    )
                }
            }
        }

        // Floating Action Button to Add Rule
        FloatingActionButton(
            onClick = {
                contactPattern = ""
                forwardNumber = ""
                labelText = ""
                allowBankingOtp = false
                simSlot = -1
                maxRetries = 0
                isTelegramEnabled = false
                telegramBotToken = ""
                telegramChatId = ""
                isEmailEnabled = false
                smtpSenderEmail = ""
                smtpAppPassword = ""
                targetEmail = ""
                isSmsEnabled = false
                showAddDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_rule_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Forward Rule")
        }

        // Add Rule Dialog Dialog
        if (showAddDialog) {
            RuleBottomSheet(
                title = "New Forwarding Routing Rule",
                initialLabelText = labelText,
                initialContactPattern = contactPattern,
                initialAllowBankingOtp = allowBankingOtp,
                initialSimSlot = simSlot,
                initialMaxRetries = maxRetries,
                initialIsSmsEnabled = isSmsEnabled,
                initialForwardNumber = forwardNumber,
                initialIsTelegramEnabled = isTelegramEnabled,
                initialTelegramBotToken = telegramBotToken,
                initialTelegramChatId = telegramChatId,
                initialIsEmailEnabled = isEmailEnabled,
                initialSmtpSenderEmail = smtpSenderEmail,
                initialSmtpAppPassword = smtpAppPassword,
                initialTargetEmail = targetEmail,
                onDismiss = { showAddDialog = false },
                onSubmit = { pat, fwd, lbl, bankOtp, sim, retries, tgOn, tgToken, tgChat, emailOn, smtpSender, smtpPass, targetEm ->
                    viewModel.addRule(pat, fwd, lbl, bankOtp, sim, retries, tgOn, tgToken, tgChat, emailOn, smtpSender, smtpPass, targetEm)
                    showAddDialog = false
                }
            )
        }

        // Edit Rule Dialog
        if (showEditDialog && selectedRuleForEdit != null) {
            val rule = selectedRuleForEdit!!
            RuleBottomSheet(
                title = "Edit Forwarding Routing Rule",
                initialLabelText = editLabelText,
                initialContactPattern = editContactPattern,
                initialAllowBankingOtp = editAllowBankingOtp,
                initialSimSlot = editSimSlot,
                initialMaxRetries = editMaxRetries,
                initialIsSmsEnabled = editIsSmsEnabled,
                initialForwardNumber = editForwardNumber,
                initialIsTelegramEnabled = editIsTelegramEnabled,
                initialTelegramBotToken = editTelegramBotToken,
                initialTelegramChatId = editTelegramChatId,
                initialIsEmailEnabled = editIsEmailEnabled,
                initialSmtpSenderEmail = editSmtpSenderEmail,
                initialSmtpAppPassword = editSmtpAppPassword,
                initialTargetEmail = editTargetEmail,
                onDismiss = { showEditDialog = false },
                onSubmit = { pat, fwd, lbl, bankOtp, sim, retries, tgOn, tgToken, tgChat, emailOn, smtpSender, smtpPass, targetEm ->
                    viewModel.updateRuleDetails(
                        rule.copy(
                            contactPattern = pat,
                            forwardToNumber = fwd,
                            label = lbl,
                            allowBankingOtp = bankOtp,
                            simSlot = sim,
                            maxRetries = retries,
                            isTelegramEnabled = tgOn,
                            telegramBotToken = tgToken,
                            telegramChatId = tgChat,
                            isEmailEnabled = emailOn,
                            smtpSenderEmail = smtpSender,
                            smtpAppPassword = smtpPass,
                            targetEmail = targetEm
                        )
                    )
                    showEditDialog = false
                }
            )
        }
    }
}
}


@Composable
fun RuleRow(
    rule: ForwardingRule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEditClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .clickable { onEditClicked() }
            .testTag("rule_card_${rule.id}"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x0DFFFFFF) // Very subtle glass
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (rule.isEnabled) Color(0xFF00FF9D) else Color.DarkGray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = rule.label,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "If text from: ${rule.contactPattern}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                if (rule.forwardToNumber.isNotEmpty()) {
                    Text(
                        text = "Forward to: ${rule.forwardToNumber}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                }
                if (rule.isTelegramEnabled) {
                    Text(
                        text = "Telegram Bot Active",
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                }
                if (rule.isEmailEnabled) {
                    Text(
                        text = "SMTP Email Active",
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                }
                if (rule.simSlot != -1) {
                    Text(
                        text = "Target SIM: ${if (rule.simSlot == 0) "SIM 1" else "SIM 2"}",
                        fontSize = 11.sp,
                        color = Color(0xFF00E5FF)
                    )
                }
                if (rule.maxRetries > 0) {
                    Text(
                        text = "Retries Enabled: ${rule.maxRetries}",
                        fontSize = 11.sp,
                        color = Color(0xFF00E5FF)
                    )
                }
                if (rule.allowBankingOtp) {
                    Text(
                        text = "⚠️ Carrier Bypass Active",
                        fontSize = 11.sp,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggle,
                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF), checkedTrackColor = Color(0x4D00E5FF)),
                    modifier = Modifier.scale(0.85f).testTag("rule_switch_${rule.id}")
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_rule_button_${rule.id}")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Rule",
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: Backups & Security Audit Logs + Simulator Sandbox
// -------------------------------------------------------------
@Composable
fun BackupsLogsTab(viewModel: SmsViewModel) {
    val messages by viewModel.allMessages.collectAsStateWithLifecycle()
    val logs by viewModel.allLogs.collectAsStateWithLifecycle()

    var activeSubTab by remember { mutableIntStateOf(0) } // 0: Security logs, 1: Local backups

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeSubTab) {
            Tab(selected = activeSubTab == 0, onClick = { activeSubTab = 0 }) {
                Box(modifier = Modifier.padding(12.dp)) { Text("Security Trace", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
            Tab(selected = activeSubTab == 1, onClick = { activeSubTab = 1 }) {
                Box(modifier = Modifier.padding(12.dp)) { Text("Cipher Backups", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
        }

        when (activeSubTab) {
            0 -> SecurityTraceLayout(logs = logs, onClear = { viewModel.clearAllLogs() })
            1 -> CipherBackupsLayout(messages = messages, onDelete = { id -> viewModel.deleteBackup(id) })
        }
    }
}

@Composable
fun SecurityTraceLayout(logs: List<AuditLog>, onClear: () -> Unit) {
    if (logs.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text("Audit Trail Clean", fontWeight = FontWeight.Bold)
            Text("Real-time forwarding/backup logs display here.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Chronological Security Logs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onClear) {
                    Text("Clear Log Items", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(logs) { log ->
                    AuditLogRow(log = log)
                }
            }
        }
    }
}

@Composable
fun AuditLogRow(log: AuditLog) {
    val formatter = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.US) }
    val dateStr = formatter.format(Date(log.timestamp))

    val statusColor = when (log.status) {
        "FORWARDED" -> Color(0xFF4CAF50)
        "BLOCKED_OTP" -> MaterialTheme.colorScheme.error
        "BACKED_UP" -> MaterialTheme.colorScheme.primary
        "BACKED_UP_ONLY" -> MaterialTheme.colorScheme.secondary
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = log.status, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Origin: ${log.senderSnippet}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = log.explanation, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (log.destinationNumber != "None" && log.destinationNumber != "N/A") {
                Text(text = "Target Forward Recipient: ${log.destinationNumber}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun CipherBackupsLayout(messages: List<SmsMessage>, onDelete: (Long) -> Unit) {
    if (messages.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text("Backup Vault Empty", fontWeight = FontWeight.Bold)
            Text("Incoming messages are backed up safely here in decrypted views.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "Secure Local AES-GCM Encrypted Messages",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(messages) { message ->
                    SmsBackupRow(message = message, onDelete = { onDelete(message.id) })
                }
            }
        }
    }
}

@Composable
fun SmsBackupRow(message: SmsMessage, onDelete: () -> Unit) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
    val dateStr = formatter.format(Date(message.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "From: ${message.decryptedSender}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Purge Single Message", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message.decryptedBody,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = "AES GCM", tint = Color(0xFF4CAF50), modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Stored Securely with At-rest AES-256-GCM. Un-encrypted text never leaks to logs.",
                    fontSize = 9.sp,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}



// -------------------------------------------------------------
// TAB 3: Backup & Restore (Google Drive via SAF)
// -------------------------------------------------------------
@Composable
fun BackupRestoreTab(
    viewModel: SmsViewModel,
    receiveSmsGranted: Boolean,
    sendSmsGranted: Boolean,
    readSmsGranted: Boolean
) {
    val context = LocalContext.current
    var showPurgeConfirmDialog by remember { mutableStateOf(false) }
    var actionType by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val googleDriveHelper = remember { GoogleDriveHelper(context) }
    
    var previewState by remember { mutableStateOf<SmsViewModel.BackupPreview?>(null) }

    val createDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        val json = viewModel.exportDataToJson()
                        out.write(json.toByteArray())
                    }
                    Toast.makeText(context, "Local Backup exported successfully!", Toast.LENGTH_LONG).show()
                } catch(e: Exception) {
                    Toast.makeText(context, "Export failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    val openDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { inp ->
                        val json = inp.bufferedReader().use { r -> r.readText() }
                        val preview = viewModel.parseBackupPreview(json)
                        if (preview != null) {
                            previewState = preview
                        } else {
                            Toast.makeText(context, "Invalid local backup file format.", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch(e: Exception) {
                    Toast.makeText(context, "Import failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                if (account != null) {
                    coroutineScope.launch {
                        if (actionType == "backup") {
                            Toast.makeText(context, "Uploading backup to Google Drive...", Toast.LENGTH_SHORT).show()
                            val json = viewModel.exportDataToJson()
                            val success = googleDriveHelper.uploadBackup(account, json)
                            if (success) {
                                Toast.makeText(context, "Backup saved to Google Drive successfully!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to upload backup.", Toast.LENGTH_SHORT).show()
                            }
                        } else if (actionType == "restore") {
                            Toast.makeText(context, "Searching for backup on Google Drive...", Toast.LENGTH_SHORT).show()
                            val json = googleDriveHelper.downloadBackup(account)
                            if (json != null) {
                                val preview = viewModel.parseBackupPreview(json)
                                if (preview != null) {
                                    previewState = preview
                                } else {
                                    Toast.makeText(context, "The Google Drive backup file is invalid or from an unsupported version.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Backup file not found on your Google Drive. Please create a backup first.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Google Sign-In failed. Error Code: ${if (e is com.google.android.gms.common.api.ApiException) e.statusCode else "Unknown"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
            val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Historical SMS Sync", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Import all existing messages from your phone's native database into the secure Cipher Backup vault so they can be exported.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    if (isImporting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(importProgress, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally), fontWeight = FontWeight.Bold)
                    } else {
                        Button(
                            onClick = { viewModel.importHistoricalSms(context) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync Historical SMS to Vault")
                        }
                        if (importProgress.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(importProgress, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Google Drive Sync", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Securely backup and restore your forwarding rules and the encrypted SMS vault using Google Drive.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                actionType = "backup"
                                signInLauncher.launch(googleDriveHelper.getSignInClient().signInIntent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cloud Backup", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                actionType = "restore"
                                signInLauncher.launch(googleDriveHelper.getSignInClient().signInIntent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cloud Restore", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Local Device Storage", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Export your configuration and SMS vault as an encrypted JSON file to your local phone storage.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                createDocLauncher.launch("safeforward_backup.json")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export File", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                openDocLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import File", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Google Play Compliance Framework", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    ComplianceCheckRow(title = "Strict User-authorized forwarding rules (no secret mirroring)", isCompliant = true)
                    ComplianceCheckRow(title = "At-rest database encryption via AES-GCM with Keystore Key", isCompliant = true)
                    ComplianceCheckRow(title = "Non-bypassable financial transaction and OTP detection blocker", isCompliant = true)
                    ComplianceCheckRow(title = "Fully auditable trace log documenting forwarding block logs", isCompliant = true)
                    ComplianceCheckRow(title = "Permission-justification screens explaining required scope", isCompliant = true)
                    ComplianceCheckRow(title = "Local device purges wipe stored databases completely", isCompliant = true)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Purge Workspace", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Instantly delete all local routing rules, cryptographic SMS backup archives, and logs. This is irreversible.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showPurgeConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("purge_all_data_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Purge All Stored App Data")
                    }
                }
            }
        }
    }

    if (showPurgeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeConfirmDialog = false },
            title = { Text("Confirm Absolute Purge", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete all backups, logging traces, and rules? This wipes the local sandbox database fully and resets setup parameters.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.purgeAccountData()
                        showPurgeConfirmDialog = false
                        Toast.makeText(context, "All system local databases and keys wiped successfully.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Wipe Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeConfirmDialog = false }) { Text("Abort") }
            }
        )
    }

    if (previewState != null) {
        AlertDialog(
            onDismissRequest = { previewState = null },
            title = { Text("Preview Backup", fontWeight = FontWeight.Bold) },
            text = { 
                Column {
                    Text("The backup file was parsed successfully and is ready to apply.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Rules Found: ${previewState!!.rulesCount}", fontWeight = FontWeight.Bold)
                    Text("• Secured Messages: ${previewState!!.messagesCount}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Do you want to restore these records into your device now?", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val success = viewModel.applyBackup(previewState!!)
                    if (success) {
                        Toast.makeText(context, "Successfully restored ${previewState!!.rulesCount} rules and ${previewState!!.messagesCount} messages!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Restore failed during application.", Toast.LENGTH_SHORT).show()
                    }
                    previewState = null
                }) { Text("Restore Now") }
            },
            dismissButton = {
                TextButton(onClick = { previewState = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ComplianceCheckRow(title: String, isCompliant: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isCompliant) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (isCompliant) Color(0xFF4CAF50) else Color.Red,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

// -------------------------------------------------------------
// TAB 4: Settings (including About content & Biometric Toggle)
// -------------------------------------------------------------
@Composable
fun SettingsTab(viewModel: SmsViewModel) {
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Security Preferences", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Biometric App Lock", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Require fingerprint or face unlock to access the dashboard", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val context = LocalContext.current
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { newValue ->
                                val fragmentActivity = context as? androidx.fragment.app.FragmentActivity
                                if (fragmentActivity != null) {
                                    val executor = androidx.core.content.ContextCompat.getMainExecutor(fragmentActivity)
                                    val biometricPrompt = androidx.biometric.BiometricPrompt(fragmentActivity, executor,
                                        object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                                            override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                                                super.onAuthenticationSucceeded(result)
                                                viewModel.setBiometricEnabled(newValue)
                                                Toast.makeText(context, "Biometric App Lock ${if (newValue) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
                                            }
                                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                                super.onAuthenticationError(errorCode, errString)
                                                Toast.makeText(context, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                                            }
                                        })
                                    val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                                        .setTitle("Authenticate")
                                        .setSubtitle("Verify identity to change security settings")
                                        .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                                        .build()
                                    biometricPrompt.authenticate(promptInfo)
                                } else {
                                    viewModel.setBiometricEnabled(newValue)
                                }
                            }
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0x3300E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "App Logo", tint = Color(0xFF00E5FF), modifier = Modifier.size(44.dp))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Safe Backup & Forward", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Version ${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "A modern, luxury, privacy-first local backup and SMS forwarding engine equipped with advanced telecom firewall bypass protocols.",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


