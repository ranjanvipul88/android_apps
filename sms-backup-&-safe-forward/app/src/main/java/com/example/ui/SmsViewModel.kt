package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ForwardingRule
import com.example.data.Repository
import com.example.receiver.SmsReceiver
import com.example.security.SmsSecurityAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SmsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: Repository
    private val prefs = application.getSharedPreferences("sms_backup_prefs", Context.MODE_PRIVATE)

    // UI state flows
    val allMessages: StateFlow<List<com.example.data.SmsMessage>>
    val allRules: StateFlow<List<ForwardingRule>>
    val allLogs: StateFlow<List<com.example.data.AuditLog>>

    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean("has_completed_onboarding", false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()
    
    private val _isBiometricOnboardingCompleted = MutableStateFlow(prefs.getBoolean("biometric_onboarding_done", false))
    val isBiometricOnboardingCompleted: StateFlow<Boolean> = _isBiometricOnboardingCompleted.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean("biometric_enabled", false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isCloudSyncEnabled = MutableStateFlow(prefs.getBoolean("cloud_sync_enabled", false))
    val isCloudSyncEnabled: StateFlow<Boolean> = _isCloudSyncEnabled.asStateFlow()

    private val _cloudProviderName = MutableStateFlow(prefs.getString("cloud_provider", "Google Drive") ?: "Google Drive")
    val cloudProviderName: StateFlow<String> = _cloudProviderName.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importProgress = MutableStateFlow("")
    val importProgress: StateFlow<String> = _importProgress.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = Repository(db)

        allMessages = repository.allMessages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allRules = repository.allRules.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allLogs = repository.allLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("has_completed_onboarding", true).apply()
        _isOnboardingCompleted.value = true
        viewModelScope.launch {
            repository.logAction(
                sender = "System",
                status = "ONBOARDED",
                explanation = "Completed privacy onboarding, reviewed permissions & security commitments.",
                destination = "N/A"
            )
        }
    }

    fun completeBiometricOnboarding(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_onboarding_done", true)
             .putBoolean("biometric_enabled", enabled).apply()
        _isBiometricOnboardingCompleted.value = true
        _isBiometricEnabled.value = enabled
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun resetOnboarding() {
        prefs.edit().remove("has_completed_onboarding")
             .remove("biometric_onboarding_done")
             .remove("biometric_enabled").apply()
        _isOnboardingCompleted.value = false
        _isBiometricOnboardingCompleted.value = false
        _isBiometricEnabled.value = false
    }

    fun importHistoricalSms(context: Context) {
        if (_isImporting.value) return
        _isImporting.value = true
        _importProgress.value = "Starting import..."

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val existingTimestamps = allMessages.value.map { it.timestamp }.toSet()
                var importedCount = 0
                var skippedCount = 0

                val uri = android.net.Uri.parse("content://sms")
                val projection = arrayOf("address", "body", "date")
                val cursor = context.contentResolver.query(uri, projection, null, null, "date ASC")

                cursor?.use { c ->
                    val total = c.count
                    val addressIdx = c.getColumnIndex("address")
                    val bodyIdx = c.getColumnIndex("body")
                    val dateIdx = c.getColumnIndex("date")

                    while (c.moveToNext()) {
                        val date = if (dateIdx >= 0) c.getLong(dateIdx) else 0L
                        if (existingTimestamps.contains(date)) {
                            skippedCount++
                        } else {
                            val address = if (addressIdx >= 0) c.getString(addressIdx) ?: "Unknown" else "Unknown"
                            val body = if (bodyIdx >= 0) c.getString(bodyIdx) ?: "" else ""

                            if (body.isNotBlank()) {
                                val encryptedSender = com.example.data.EncryptionHelper.encrypt(address)
                                val encryptedBody = com.example.data.EncryptionHelper.encrypt(body)
                                val msg = com.example.data.SmsMessage(
                                    encryptedSender = encryptedSender,
                                    encryptedBody = encryptedBody,
                                    timestamp = date,
                                    isBackupSentToCloud = false,
                                    cloudProvider = null
                                )
                                repository.insertMessage(msg)
                                importedCount++
                            } else {
                                skippedCount++
                            }
                        }

                        if ((importedCount + skippedCount) % 100 == 0) {
                            _importProgress.value = "Processed ${importedCount + skippedCount} / $total..."
                        }
                    }
                }
                _importProgress.value = "Import complete! Added $importedCount new messages. Skipped $skippedCount existing."
            } catch (e: Exception) {
                e.printStackTrace()
                _importProgress.value = "Import failed: ${e.message}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun toggleCloudSync(enabled: Boolean) {
        prefs.edit().putBoolean("cloud_sync_enabled", enabled).apply()
        _isCloudSyncEnabled.value = enabled
        viewModelScope.launch {
            repository.logAction(
                sender = "BackupManager",
                status = "SYNC_CONFIG",
                explanation = "Cloud backup sync ${if (enabled) "ENABLED" else "DISABLED"} for: ${_cloudProviderName.value}.",
                destination = "N/A"
            )
        }
    }

    fun setCloudProvider(providerName: String) {
        prefs.edit().putString("cloud_provider", providerName).apply()
        _cloudProviderName.value = providerName
    }

    fun addRule(
        pattern: String, destination: String, label: String, allowBankingOtp: Boolean, simSlot: Int, maxRetries: Int,
        isTelegramEnabled: Boolean, telegramBotToken: String, telegramChatId: String,
        isEmailEnabled: Boolean, smtpSenderEmail: String, smtpAppPassword: String, targetEmail: String
    ) {
        viewModelScope.launch {
            repository.createRule(pattern, destination, label, allowBankingOtp, simSlot, maxRetries, isTelegramEnabled, telegramBotToken, telegramChatId, isEmailEnabled, smtpSenderEmail, smtpAppPassword, targetEmail)
            repository.logAction(
                sender = "System",
                status = "RULE_ADD",
                explanation = "Created forwarding rule for dynamic query '$pattern' targeting '$destination' (Allow Banking/OTP: $allowBankingOtp).",
                destination = destination
            )
        }
    }

    fun updateRuleDetails(rule: ForwardingRule) {
        viewModelScope.launch {
            repository.updateRule(rule)
            repository.logAction(
                sender = "System",
                status = "RULE_UPDATE",
                explanation = "Updated rule '${rule.label}' content details (Allow Banking/OTP: ${rule.allowBankingOtp}).",
                destination = rule.forwardToNumber
            )
        }
    }

    fun toggleRule(rule: ForwardingRule, enabled: Boolean) {
        viewModelScope.launch {
            repository.updateRule(rule.copy(isEnabled = enabled))
            repository.logAction(
                sender = "System",
                status = "RULE_UPDATE",
                explanation = "Rule '${rule.label}' forwarding to ${rule.forwardToNumber} was ${if (enabled) "ENABLED" else "DISABLED"}.",
                destination = rule.forwardToNumber
            )
        }
    }

    fun deleteRule(rule: ForwardingRule) {
        viewModelScope.launch {
            repository.deleteRule(rule)
            repository.logAction(
                sender = "System",
                status = "RULE_DELETE",
                explanation = "Deleted rule forwarding pattern '${rule.contactPattern}' to ${rule.forwardToNumber}.",
                destination = rule.forwardToNumber
            )
        }
    }

    fun deleteBackup(id: Long) {
        viewModelScope.launch {
            repository.deleteMessage(id)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
        }
    }

    fun purgeAccountData() {
        viewModelScope.launch {
            repository.purgeAllData()
            // Reset state
            prefs.edit().clear().apply()
            _isOnboardingCompleted.value = false
            _isCloudSyncEnabled.value = false
            _cloudProviderName.value = "Google Drive"
        }
    }

    /**
     * Simulation engine for QA Sandbox.
     * Replicates receiving an SMS and walking through backup, pattern checks, security block evaluation,
     * and simulated transmission logs. This allows direct browser-emulator validation.
     */
    fun simulateSmsReceived(sender: String, body: String) {
        viewModelScope.launch {
            if (body.isBlank()) return@launch

            val normalSender = sender.trim().ifEmpty { "SystemTest" }

            // 1. Local backup & secure audit trail logging
            repository.backUpMessage(normalSender, body)
            repository.logAction(
                sender = normalSender,
                status = "BACKED_UP",
                explanation = "[Simulation] SMS received and stored in AES-GCM encrypted local storage.",
                destination = "None"
            )

            // 2. Fetch active routing patterns
            val activeRules = repository.getActiveRules()
            var matched = false

            for (rule in activeRules) {
                val pattern = rule.contactPattern
                val destination = rule.forwardToNumber

                val match = if (pattern.equals("All Contacts/Numbers", ignoreCase = true) || pattern == "*" || pattern.isBlank()) {
                    true
                } else {
                    normalSender.replace(" ", "").contains(pattern.replace(" ", ""))
                }

                if (match) {
                    matched = true
                    // 3. Security Exclusion Analysis
                    val analysis = SmsSecurityAnalyzer.analyze(body)
                    val isBypassed = !analysis.isSafe && rule.allowBankingOtp

                    if (!analysis.isSafe && !rule.allowBankingOtp) {
                        repository.logAction(
                            sender = normalSender,
                            status = "BLOCKED_OTP",
                            explanation = "[Simulation] Forward blocked. Message triggers safety exclusion (Banking/OTP) and bypass is disabled: ${analysis.matchedPattern}",
                            destination = destination
                        )
                    } else {
                        val bypassText = if (isBypassed) " [BYPASS: Banking/OTP Allowed: ${analysis.matchedPattern}]" else ""
                        // Safe forward simulated
                        repository.logAction(
                            sender = normalSender,
                            status = "FORWARDED",
                            explanation = "[Simulation] Forwarding executed under filter: '${rule.label}'$bypassText.",
                            destination = destination
                        )
                    }
                }
            }

            if (!matched) {
                repository.logAction(
                    sender = normalSender,
                    status = "BACKED_UP_ONLY",
                    explanation = "[Simulation] Incoming SMS does not match any active forwarding patterns. Kept in secure local backup storage only.",
                    destination = "None"
                )
            }
        }
    }

    data class BackupPreview(
        val version: Int,
        val rulesCount: Int,
        val messagesCount: Int,
        val rawJson: String
    )

    fun exportDataToJson(): String {
        val rules = allRules.value
        val messages = allMessages.value
        val rulesArray = org.json.JSONArray()
        for (rule in rules) {
            val obj = org.json.JSONObject()
            obj.put("contactPattern", rule.contactPattern)
            obj.put("forwardToNumber", rule.forwardToNumber)
            obj.put("isEnabled", rule.isEnabled)
            obj.put("label", rule.label)
            obj.put("allowBankingOtp", rule.allowBankingOtp)
            obj.put("simSlot", rule.simSlot)
            obj.put("maxRetries", rule.maxRetries)
            obj.put("isTelegramEnabled", rule.isTelegramEnabled)
            obj.put("telegramBotToken", rule.telegramBotToken)
            obj.put("telegramChatId", rule.telegramChatId)
            obj.put("isEmailEnabled", rule.isEmailEnabled)
            obj.put("smtpSenderEmail", rule.smtpSenderEmail)
            obj.put("smtpAppPassword", rule.smtpAppPassword)
            obj.put("targetEmail", rule.targetEmail)
            rulesArray.put(obj)
        }
        val messagesArray = org.json.JSONArray()
        for (msg in messages) {
            val obj = org.json.JSONObject()
            obj.put("encryptedSender", msg.encryptedSender)
            obj.put("encryptedBody", msg.encryptedBody)
            obj.put("timestamp", msg.timestamp)
            obj.put("isBackupSentToCloud", msg.isBackupSentToCloud)
            obj.put("cloudProvider", msg.cloudProvider ?: org.json.JSONObject.NULL)
            messagesArray.put(obj)
        }

        val wrapper = org.json.JSONObject()
        wrapper.put("version", 2)
        wrapper.put("rules", rulesArray)
        wrapper.put("messages", messagesArray)
        return wrapper.toString(4)
    }

    fun parseBackupPreview(jsonString: String): BackupPreview? {
        return try {
            val wrapper = org.json.JSONObject(jsonString)
            val version = wrapper.optInt("version", 1)
            val rulesArray = wrapper.optJSONArray("rules")
            val rulesCount = rulesArray?.length() ?: 0
            val messagesArray = wrapper.optJSONArray("messages")
            val messagesCount = messagesArray?.length() ?: 0
            BackupPreview(version, rulesCount, messagesCount, jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun applyBackup(preview: BackupPreview): Boolean {
        try {
            val wrapper = org.json.JSONObject(preview.rawJson)
            val rulesArray = wrapper.optJSONArray("rules")
            if (rulesArray != null) {
                for (i in 0 until rulesArray.length()) {
                    val obj = rulesArray.getJSONObject(i)
                    val rule = ForwardingRule(
                        contactPattern = obj.optString("contactPattern", ""),
                        forwardToNumber = obj.optString("forwardToNumber", ""),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        label = obj.optString("label", ""),
                        allowBankingOtp = obj.optBoolean("allowBankingOtp", false),
                        simSlot = obj.optInt("simSlot", -1),
                        maxRetries = obj.optInt("maxRetries", 0),
                        isTelegramEnabled = obj.optBoolean("isTelegramEnabled", false),
                        telegramBotToken = obj.optString("telegramBotToken", ""),
                        telegramChatId = obj.optString("telegramChatId", ""),
                        isEmailEnabled = obj.optBoolean("isEmailEnabled", false),
                        smtpSenderEmail = obj.optString("smtpSenderEmail", ""),
                        smtpAppPassword = obj.optString("smtpAppPassword", ""),
                        targetEmail = obj.optString("targetEmail", "")
                    )
                    viewModelScope.launch {
                        repository.createRule(
                            rule.contactPattern, rule.forwardToNumber, rule.label, rule.allowBankingOtp,
                            rule.simSlot, rule.maxRetries, rule.isTelegramEnabled, rule.telegramBotToken, rule.telegramChatId,
                            rule.isEmailEnabled, rule.smtpSenderEmail, rule.smtpAppPassword, rule.targetEmail
                        )
                    }
                }
            }
            
            val messagesArray = wrapper.optJSONArray("messages")
            if (messagesArray != null) {
                for (i in 0 until messagesArray.length()) {
                    val obj = messagesArray.getJSONObject(i)
                    val msg = com.example.data.SmsMessage(
                        encryptedSender = obj.getString("encryptedSender"),
                        encryptedBody = obj.getString("encryptedBody"),
                        timestamp = obj.getLong("timestamp"),
                        isBackupSentToCloud = obj.optBoolean("isBackupSentToCloud", false),
                        cloudProvider = if (obj.has("cloudProvider") && !obj.isNull("cloudProvider")) obj.getString("cloudProvider") else null
                    )
                    viewModelScope.launch {
                        repository.insertMessage(msg)
                    }
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
