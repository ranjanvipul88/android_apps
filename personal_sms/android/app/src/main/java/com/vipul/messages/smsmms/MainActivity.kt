package com.vipul.messages.smsmms

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.vipul.messages.smsmms.data.SmsDatabase
import com.vipul.messages.smsmms.data.SmsMessage
import com.vipul.messages.smsmms.ui.SmsReplicaTheme
import com.vipul.messages.smsmms.worker.SmsSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var database: SmsDatabase

    // Live state streams of conversations list
    private var messagesState = mutableStateListOf<SmsMessage>()
    private var isDefaultSmsState = mutableStateOf(false)

    // Request permissions launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.RECEIVE_SMS] == true &&
                         permissions[Manifest.permission.SEND_SMS] == true &&
                         permissions[Manifest.permission.READ_SMS] == true
        if (smsGranted) {
            checkDefaultSmsHandler()
            loadLocalHistory()
        } else {
            Toast.makeText(this, "SMS permissions are required to operate.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPrefs = getSharedPreferences("sms_replica_prefs", Context.MODE_PRIVATE)
        database = SmsDatabase.getDatabase(this)

        checkPermissions()
        setupBackgroundSyncWorker()

        setContent {
            SmsReplicaTheme {
                MainOrchestratorView()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkDefaultSmsHandler()
        loadLocalHistory()
    }

    private fun checkPermissions() {
        val required = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_MMS,
            Manifest.permission.RECEIVE_WAP_PUSH
        )
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            requestPermissionLauncher.launch(missing.toTypedArray())
        } else {
            checkDefaultSmsHandler()
            loadLocalHistory()
        }
    }

    private fun checkDefaultSmsHandler() {
        val defaultPackage = Telephony.Sms.getDefaultSmsPackage(this)
        isDefaultSmsState.value = defaultPackage == packageName
    }

    private fun requestDefaultSmsHandler() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                @Suppress("DEPRECATION")
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                startActivityForResult(intent, 1001)
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            }
            startActivity(intent)
        }
    }

    private fun loadLocalHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            database.smsDao().getAllMessages().collect { list ->
                withContext(Dispatchers.Main) {
                    messagesState.clear()
                    messagesState.addAll(list)
                    
                    // Seed some initial chat helpers if database is empty
                    if (messagesState.isEmpty()) {
                        seedLocalHistory()
                    }
                }
            }
        }
    }

    private fun seedLocalHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val initialHistory = listOf(
                SmsMessage(
                    message_id = "SEED-1",
                    phone_number = "+15551234567",
                    sender_name = "Mom ❤️",
                    body = "Hi honey! Just checking in. Let me know when you get home so we can have dinner 🥧",
                    timestamp = now - 3600000 * 2,
                    direction = "incoming"
                ),
                SmsMessage(
                    message_id = "SEED-2",
                    phone_number = "+15559876543",
                    sender_name = "Project Manager 💼",
                    body = "We need the release candidates built today. Verify if the SMS/MMS permissions look sound for the Play Store submission.",
                    timestamp = now - 3600000,
                    direction = "incoming"
                ),
                SmsMessage(
                    message_id = "SEED-3",
                    phone_number = "+15553141592",
                    sender_name = "Tech Geek 🤖",
                    body = "The database uses room local caching paired with rapid background synchronization workers. Tap on settings to pair your web portal!",
                    timestamp = now - 1800000,
                    direction = "incoming"
                )
            )
            database.smsDao().insertMessages(initialHistory)
        }
    }

    private fun setupBackgroundSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SmsSyncWorker>(
            15, TimeUnit.MINUTES // Standard Android background worker interval
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "sms_cloud_synchronizer",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    // ==========================================================================
    // JETPACK COMPOSE UI CONTROLLERS
    // ==========================================================================

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainOrchestratorView() {
        var currentScreen by remember { mutableStateOf("threads") } // threads, chat, settings
        var activeContactPhone by remember { mutableStateOf("") }
        var activeContactName by remember { mutableStateOf("") }

        val isDefaultSms by isDefaultSmsState

        Scaffold(
            bottomBar = {
                if (currentScreen != "chat") {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        NavigationBarItem(
                            selected = currentScreen == "threads",
                            onClick = { currentScreen = "threads" },
                            icon = { Icon(Icons.Filled.Forum, contentDescription = "Conversations") },
                            label = { Text("Chats") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == "settings",
                            onClick = { currentScreen = "settings" },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (currentScreen) {
                    "threads" -> ConversationsListScreen(
                        isDefault = isDefaultSms,
                        onRequestDefault = { requestDefaultSmsHandler() },
                        onThreadSelected = { phone, name ->
                            activeContactPhone = phone
                            activeContactName = name
                            currentScreen = "chat"
                        }
                    )
                    "chat" -> ChatConversationScreen(
                        contactPhone = activeContactPhone,
                        contactName = activeContactName,
                        onBack = { currentScreen = "threads" }
                    )
                    "settings" -> SettingsConfigScreen()
                }
            }
        }
    }

    // SCREEN 1: Conversations List view
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ConversationsListScreen(
        isDefault: Boolean,
        onRequestDefault: () -> Unit,
        onThreadSelected: (String, String) -> Unit
    ) {
        var searchQuery by remember { mutableStateOf("") }
        
        // Group messages by contact to form threads
        val threads = remember(messagesState, searchQuery) {
            val grouped = messagesState.groupBy { it.phone_number }
            grouped.map { (phone, msgs) ->
                val lastMsg = msgs.maxByOrNull { it.timestamp }!!
                lastMsg
            }.filter {
                searchQuery.isEmpty() || 
                it.body.contains(searchQuery, ignoreCase = true) ||
                it.phone_number.contains(searchQuery) ||
                it.sender_name.contains(searchQuery, ignoreCase = true)
            }.sortedByDescending { it.timestamp }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Material 3 Dynamic Custom Header with specialized cell logo
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "App Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Messages", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                }
            )

            // Warning Banner for setting Default SMS application
            if (!isDefault) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Not Default App", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("Set as Default SMS handler to manage cellular messages on PlayStore.", fontSize = 12.sp)
                        }
                        Button(
                            onClick = onRequestDefault,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Set")
                        }
                    }
                }
            }

            // Quick Search input field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search conversations...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // List of Conversations
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(threads) { thread ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThreadSelected(thread.phone_number, thread.sender_name) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar bubble
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            val char = thread.sender_name.firstOrNull()?.uppercase() ?: "?"
                            Text(
                                text = char,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = thread.sender_name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Just Now", // Simplified for visual elegance
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = thread.body,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 0.5.dp)
                }
            }
        }
    }

    // SCREEN 2: Conversation detail Chat screen
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ChatConversationScreen(
        contactPhone: String,
        contactName: String,
        onBack: () -> Unit
    ) {
        val chatMessages = remember(messagesState, contactPhone) {
            messagesState.filter { it.phone_number == contactPhone }.sortedBy { it.timestamp }
        }

        var typedText by remember { mutableStateOf("") }
        val listState = rememberLazyListState()

        // Auto Scroll to bottom on new messages
        LaunchedEffect(chatMessages.size) {
            if (chatMessages.isNotEmpty()) {
                listState.animateScrollToItem(chatMessages.size - 1)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Chat Header
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contactName.firstOrNull()?.uppercase() ?: "?",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(contactName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(contactPhone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Star filter */ }) {
                        Icon(Icons.Filled.Star, contentDescription = "Favorites")
                    }
                }
            )

            // Timeline message bubbles
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatMessages) { msg ->
                    val isOutgoing = msg.direction == "outgoing"
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 260.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isOutgoing) 16.dp else 4.dp,
                                        bottomEnd = if (isOutgoing) 4.dp else 16.dp
                                    )
                                )
                                .background(
                                    if (isOutgoing) MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                // Rich media photo attachments mock
                                if (msg.type == "mms" && msg.mms_attachment_url.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.DarkGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("📷 MMS Photo Attachment", fontSize = 12.sp, color = Color.White)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                Text(
                                    text = msg.body,
                                    color = if (isOutgoing) MaterialTheme.colorScheme.onPrimaryContainer 
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.5.sp
                                )
                            }
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text("12:45 PM", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) // Simulated for visual neatness
                            if (isOutgoing) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.DoneAll,
                                    contentDescription = "Delivered",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Input Send Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Emojis drawer */ }) {
                    Icon(Icons.Outlined.SentimentSatisfied, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { /* MMS media selectors */ }) {
                    Icon(Icons.Outlined.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                OutlinedTextField(
                    value = typedText,
                    onValueChange = { typedText = it },
                    placeholder = { Text("Text message (Cellular SMS)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (typedText.trim().isNotEmpty()) {
                            performOutgoingSms(contactPhone, typedText)
                            typedText = ""
                        }
                    })
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = {
                        if (typedText.trim().isNotEmpty()) {
                            performOutgoingSms(contactPhone, typedText)
                            typedText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }

    private fun performOutgoingSms(phone: String, body: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val localMsgId = "SMS-OUT-${UUID.randomUUID()}"
            val outgoingMsg = SmsMessage(
                message_id = localMsgId,
                phone_number = phone,
                sender_name = "Me",
                body = body,
                timestamp = System.currentTimeMillis(),
                direction = "outgoing",
                type = "sms",
                is_synced = false
            )
            
            // Insert locally
            database.smsDao().insertMessage(outgoingMsg)

            // Natively execute actual hardware SMS transmission
            try {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                val parts = smsManager.divideMessage(body)
                smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
                Log.d(TAG, "Hardware SMS sent successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Hardware cell SMS send failed", e)
            }

            // Instantly trigger WorkManager sync worker to sync outgoing logs up to Web Portal URL!
            val syncTrigger = OneTimeWorkRequestBuilder<SmsSyncWorker>().build()
            WorkManager.getInstance(this@MainActivity).enqueue(syncTrigger)
        }
    }


    // SCREEN 3: Settings Panel with Auto-Forwarding and Web Pairing inputs
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SettingsConfigScreen() {
        var forwardPhone by remember { mutableStateOf(sharedPrefs.getString("forward_phone", "") ?: "") }
        var isForwardEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("forward_enabled", false)) }
        
        var pairServerUrl by remember { mutableStateOf(sharedPrefs.getString("sync_server", "http://localhost:3000") ?: "http://localhost:3000") }
        var pairKeyInput by remember { mutableStateOf("") }
        var devicePairToken by remember { mutableStateOf(sharedPrefs.getString("device_token", "") ?: "") }

        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Settings Configurations", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: SMS Auto-Forwarding
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("SMS Auto-Forwarding", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Instantly forward incoming cellular texts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isForwardEnabled,
                            onCheckedChange = {
                                isForwardEnabled = it
                                sharedPrefs.edit().putBoolean("forward_enabled", it).apply()
                                Toast.makeText(context, "Auto-Forwarding toggled: $it", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = forwardPhone,
                        onValueChange = {
                            forwardPhone = it
                            sharedPrefs.edit().putString("forward_phone", it).apply()
                        },
                        label = { Text("Forward target phone number") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }
            }

            // Section 2: Web Sync pairing configuration
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Web Access Sync", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Retrieve messages via web URL link securely", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = pairServerUrl,
                        onValueChange = {
                            pairServerUrl = it
                            sharedPrefs.edit().putString("sync_server", it).apply()
                        },
                        label = { Text("Cloud Sync Server URL") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (devicePairToken.isEmpty()) {
                        OutlinedTextField(
                            value = pairKeyInput,
                            onValueChange = { pairKeyInput = it },
                            label = { Text("Pairing Key (from Web Screen)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (pairKeyInput.trim().isEmpty()) {
                                    Toast.makeText(context, "Enter valid Pair Key.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                executeMobileWebPairing(pairKeyInput, pairServerUrl) { token ->
                                    devicePairToken = token
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Pair Device")
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Device paired. Token synced successfully!", fontSize = 13.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                sharedPrefs.edit().putString("device_token", "").apply()
                                devicePairToken = ""
                            }) {
                                Text("Unpair", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Developer debug triggers panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Developer Mock Telephony", fontWeight = FontWeight.Bold)
                    Text("Trigger simulated cellular signals to test receivers", fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { simulateIncomingText("Mom ❤️", "+15551234567", "Hi sweetheart! Did you check the auto-forwarder logs? 🥧") }) {
                            Text("Mom SMS")
                        }
                        Button(onClick = { simulateIncomingText("Project Manager 💼", "+15559876543", "Build approved. Synced database shows correct values.") }) {
                            Text("Boss SMS")
                        }
                    }
                }
            }
        }
    }

    private fun simulateIncomingText(senderName: String, phone: String, body: String) {
        val intent = Intent("android.provider.Telephony.SMS_DELIVER").apply {
            setClassName(packageName, "com.vipul.messages.smsmms.receiver.SmsReceiver")
            // Package mock PDUs
            val mockPdu = byteArrayOf() // Empty mock bytes, SmsReceiver handles triggers
            putExtra("pdus", arrayOf(mockPdu))
            putExtra("format", "3gpp")
        }
        
        // Directly inject into the receiver to test local Room inserting, Auto-forwarding, and Ktor cloud Syncing!
        lifecycleScope.launch(Dispatchers.IO) {
            val localMsgId = "SMS-SIM-${UUID.randomUUID()}"
            val incomingSms = SmsMessage(
                message_id = localMsgId,
                phone_number = phone,
                sender_name = senderName,
                body = body,
                timestamp = System.currentTimeMillis(),
                direction = "incoming",
                type = "sms",
                is_synced = false
            )
            database.smsDao().insertMessage(incomingSms)
            
            // Check auto forwarding and trigger outgoing SMS if active
            val isForwardEnabled = sharedPrefs.getBoolean("forward_enabled", false)
            val forwardNumber = sharedPrefs.getString("forward_phone", "") ?: ""

            if (isForwardEnabled && forwardNumber.isNotEmpty()) {
                val fwdOutgoing = SmsMessage(
                    message_id = "FWD-SIM-OUT-${UUID.randomUUID()}",
                    phone_number = forwardNumber,
                    sender_name = "Auto-Forwarder",
                    body = "[FWD from $senderName]: $body",
                    timestamp = System.currentTimeMillis(),
                    direction = "outgoing",
                    type = "sms",
                    is_synced = false
                )
                database.smsDao().insertMessage(fwdOutgoing)
            }

            // Instantly execute sync worker
            val syncTrigger = OneTimeWorkRequestBuilder<SmsSyncWorker>().build()
            WorkManager.getInstance(this@MainActivity).enqueue(syncTrigger)
        }
        Toast.makeText(this, "Simulated SMS triggered & synced to web URL!", Toast.LENGTH_SHORT).show()
    }

    private fun executeMobileWebPairing(pairKey: String, serverUrl: String, onPaired: (String) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = io.ktor.client.HttpClient(io.ktor.client.engine.android.Android) {
                    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }
                
                val response = client.post("$serverUrl/api/sync/pair") {
                    contentType(ContentType.Application.Json)
                    setBody(PairRequest(pair_token = pairKey, battery_level = 90, network_signal = "Strong"))
                }

                if (response.status == HttpStatusCode.OK) {
                    val body = response.bodyAsText()
                    val result = Json.decodeFromString<PairResponse>(body)
                    
                    sharedPrefs.edit().apply {
                        putString("device_token", result.device_token)
                        putString("sync_server", serverUrl)
                        putBoolean("forward_enabled", result.is_forward_enabled)
                        putString("forward_phone", result.forward_number)
                        apply()
                    }

                    withContext(Dispatchers.Main) {
                        onPaired(result.device_token)
                        Toast.makeText(this@MainActivity, "Device paired successfully!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Failed pairing: ${response.status}", Toast.LENGTH_SHORT).show()
                    }
                }
                client.close()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Connection error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

// Pairing network transfer models
@Serializable
data class PairRequest(val pair_token: String, val battery_level: Int, val network_signal: String)
@Serializable
data class PairResponse(val message: String, val device_token: String, val forward_number: String, val is_forward_enabled: Boolean)
