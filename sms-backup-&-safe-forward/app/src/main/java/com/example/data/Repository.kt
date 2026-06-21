package com.example.data

import kotlinx.coroutines.flow.Flow

class Repository(private val db: AppDatabase) {

    private val smsDao = db.smsDao()
    private val ruleDao = db.ruleDao()
    private val auditDao = db.auditDao()

    val allMessages: Flow<List<SmsMessage>> = smsDao.getAllMessages()
    val allRules: Flow<List<ForwardingRule>> = ruleDao.getAllRules()
    val allLogs: Flow<List<AuditLog>> = auditDao.getAllLogs()

    suspend fun backUpMessage(sender: String, body: String): Long {
        val encryptedSender = EncryptionHelper.encrypt(sender)
        val encryptedBody = EncryptionHelper.encrypt(body)
        val sms = SmsMessage(
            encryptedSender = encryptedSender,
            encryptedBody = encryptedBody
        )
        return smsDao.insertMessage(sms)
    }

    suspend fun insertMessage(message: SmsMessage): Long {
        return smsDao.insertMessage(message)
    }

    suspend fun getActiveRules(): List<ForwardingRule> = ruleDao.getActiveRules()

    suspend fun createRule(
        contactPattern: String,
        forwardToNumber: String,
        label: String,
        allowBankingOtp: Boolean = false,
        simSlot: Int = -1,
        maxRetries: Int = 0,
        isTelegramEnabled: Boolean = false,
        telegramBotToken: String = "",
        telegramChatId: String = "",
        isEmailEnabled: Boolean = false,
        smtpSenderEmail: String = "",
        smtpAppPassword: String = "",
        targetEmail: String = ""
    ): Long {
        val rule = ForwardingRule(
            contactPattern = contactPattern.trim(),
            forwardToNumber = forwardToNumber.trim(),
            label = label.trim(),
            allowBankingOtp = allowBankingOtp,
            simSlot = simSlot,
            maxRetries = maxRetries,
            isTelegramEnabled = isTelegramEnabled,
            telegramBotToken = telegramBotToken.trim(),
            telegramChatId = telegramChatId.trim(),
            isEmailEnabled = isEmailEnabled,
            smtpSenderEmail = smtpSenderEmail.trim(),
            smtpAppPassword = smtpAppPassword.trim(),
            targetEmail = targetEmail.trim()
        )
        return ruleDao.insertRule(rule)
    }

    suspend fun updateRule(rule: ForwardingRule) {
        ruleDao.updateRule(rule)
    }

    suspend fun deleteRule(rule: ForwardingRule) {
        ruleDao.deleteRule(rule)
    }

    suspend fun logAction(sender: String, status: String, explanation: String, destination: String): Long {
        // Redact or mask the sender for privacy logs
        val maskLength = if (sender.length > 6) sender.length - 4 else sender.length / 2
        val maskedSender = if (sender.isNotEmpty()) {
            sender.take(3) + "*".repeat(maskLength) + sender.takeLast(2)
        } else {
            "Unknown"
        }
        val log = AuditLog(
            senderSnippet = maskedSender,
            status = status,
            explanation = explanation,
            destinationNumber = destination
        )
        return auditDao.insertLog(log)
    }

    suspend fun deleteMessage(id: Long) {
        smsDao.deleteMessageById(id)
    }

    suspend fun clearAllLogs() {
        auditDao.clearAllLogs()
    }

    suspend fun purgeAllData() {
        smsDao.clearAllMessages()
        ruleDao.clearAllRules()
        auditDao.clearAllLogs()
    }
}
