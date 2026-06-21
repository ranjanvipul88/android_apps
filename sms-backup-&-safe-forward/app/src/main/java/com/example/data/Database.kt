package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Entities
@Entity(tableName = "sms_messages")
data class SmsMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val encryptedSender: String,
    val encryptedBody: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isBackupSentToCloud: Boolean = false,
    val cloudProvider: String? = null
) {
    val decryptedSender: String
        get() = EncryptionHelper.decrypt(encryptedSender)

    val decryptedBody: String
        get() = EncryptionHelper.decrypt(encryptedBody)
}

@Entity(tableName = "forwarding_rules")
data class ForwardingRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactPattern: String, // e.g. "All Contacts/Numbers" or a specific number "+1234567890"
    val forwardToNumber: String, // Destination number
    val isEnabled: Boolean = true,
    val label: String = "",
    val allowBankingOtp: Boolean = false,
    val simSlot: Int = -1, // -1 = All, 0 = SIM 1, 1 = SIM 2
    val maxRetries: Int = 0, // 0 to 10
    val isTelegramEnabled: Boolean = false,
    val telegramBotToken: String = "",
    val telegramChatId: String = "",
    val isEmailEnabled: Boolean = false,
    val smtpSenderEmail: String = "",
    val smtpAppPassword: String = "",
    val targetEmail: String = ""
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val senderSnippet: String, // e.g., "+123******" for privacy, or full encrypted sender
    val status: String, // e.g., "FORWARDED", "BLOCKED_OTP", "BACKED_UP_ONLY", "ERROR"
    val explanation: String, // Rationale like "Successfully forwarded to +1987654" or "Security Alert: Blocked sensitive keyword 'OTP'"
    val destinationNumber: String // Destination if forwarded, or "N/A"
)

// 2. DAOs
@Dao
interface SmsDao {
    @Query("SELECT * FROM sms_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<SmsMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: SmsMessage): Long

    @Query("DELETE FROM sms_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    @Query("DELETE FROM sms_messages")
    suspend fun clearAllMessages()
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM forwarding_rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<ForwardingRule>>

    @Query("SELECT * FROM forwarding_rules WHERE isEnabled = 1")
    suspend fun getActiveRules(): List<ForwardingRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: ForwardingRule): Long

    @Update
    suspend fun updateRule(rule: ForwardingRule)

    @Delete
    suspend fun deleteRule(rule: ForwardingRule)

    @Query("DELETE FROM forwarding_rules")
    suspend fun clearAllRules()
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLog): Long

    @Query("DELETE FROM audit_logs")
    suspend fun clearAllLogs()
}

@Database(
    entities = [SmsMessage::class, ForwardingRule::class, AuditLog::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smsDao(): SmsDao
    abstract fun ruleDao(): RuleDao
    abstract fun auditDao(): AuditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Safely create the audit_logs table if it was added in version 2
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `audit_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        `senderSnippet` TEXT NOT NULL, 
                        `status` TEXT NOT NULL, 
                        `explanation` TEXT NOT NULL, 
                        `destinationNumber` TEXT NOT NULL
                    )
                """)

                // Safely add any new columns that might have been introduced in version 2
                try { db.execSQL("ALTER TABLE `forwarding_rules` ADD COLUMN `label` TEXT NOT NULL DEFAULT ''") } catch (e: Exception) { /* Ignore if already exists */ }
                try { db.execSQL("ALTER TABLE `forwarding_rules` ADD COLUMN `allowBankingOtp` INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) { }
                try { db.execSQL("ALTER TABLE `sms_messages` ADD COLUMN `isBackupSentToCloud` INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) { }
                try { db.execSQL("ALTER TABLE `sms_messages` ADD COLUMN `cloudProvider` TEXT") } catch (e: Exception) { }
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE `forwarding_rules` ADD COLUMN `simSlot` INTEGER NOT NULL DEFAULT -1") } catch (e: Exception) { }
                try { db.execSQL("ALTER TABLE `forwarding_rules` ADD COLUMN `maxRetries` INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) { }
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE `forwarding_rules` ADD COLUMN `isTelegramEnabled` INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) { }
                try { db.execSQL("ALTER TABLE `forwarding_rules` ADD COLUMN `telegramBotToken` TEXT NOT NULL DEFAULT ''") } catch (e: Exception) { }
                try { db.execSQL("ALTER TABLE `forwarding_rules` ADD COLUMN `telegramChatId` TEXT NOT NULL DEFAULT ''") } catch (e: Exception) { }
                try { db.execSQL("ALTER TABLE `forwarding_rules` ADD COLUMN `isEmailEnabled` INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) { }
                try { db.execSQL("ALTER TABLE `forwarding_rules` ADD COLUMN `smtpSenderEmail` TEXT NOT NULL DEFAULT ''") } catch (e: Exception) { }
                try { db.execSQL("ALTER TABLE `forwarding_rules` ADD COLUMN `smtpAppPassword` TEXT NOT NULL DEFAULT ''") } catch (e: Exception) { }
                try { db.execSQL("ALTER TABLE `forwarding_rules` ADD COLUMN `targetEmail` TEXT NOT NULL DEFAULT ''") } catch (e: Exception) { }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sms_backup_forward_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
