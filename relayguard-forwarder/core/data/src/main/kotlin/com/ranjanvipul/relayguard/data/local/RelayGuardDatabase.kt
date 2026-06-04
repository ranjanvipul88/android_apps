package com.ranjanvipul.relayguard.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "filters")
data class FilterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val enabled: Boolean,
    val messageKinds: String,
    val conditionsJson: String,
    val recipientsJson: String,
    val template: String,
    val replacementsJson: String,
    val scheduleJson: String?,
    val allowSensitiveMessages: Boolean,
    val preventRapidDuplicates: Boolean
)

@Entity(tableName = "message_events")
data class MessageEventEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val sender: String,
    val body: String,
    val receivedAtEpochMillis: Long,
    val simSlot: String?,
    val packageName: String?,
    val notificationTitle: String?,
    val attachmentCount: Int
)

@Entity(tableName = "relay_attempts")
data class RelayAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filterId: String,
    val recipientId: String,
    val recipientKind: String,
    val bodyPreview: String,
    val success: Boolean,
    val detail: String?,
    val createdAtEpochMillis: Long
)

@Dao
interface FilterDao {
    @Query("SELECT * FROM filters ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<FilterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FilterEntity)

    @Query("UPDATE filters SET enabled = :enabled WHERE id = :filterId")
    suspend fun setEnabled(filterId: String, enabled: Boolean)

    @Query("DELETE FROM filters WHERE id = :filterId")
    suspend fun delete(filterId: String)
}

@Dao
interface MessageLogDao {
    @Query("SELECT * FROM message_events ORDER BY receivedAtEpochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<MessageEventEntity>>

    @Query("SELECT * FROM relay_attempts ORDER BY createdAtEpochMillis DESC LIMIT :limit")
    fun observeRecentRelays(limit: Int): Flow<List<RelayAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(entity: MessageEventEntity)

    @Insert
    suspend fun insertAttempt(entity: RelayAttemptEntity)
}

@Database(
    entities = [FilterEntity::class, MessageEventEntity::class, RelayAttemptEntity::class],
    version = 1,
    exportSchema = true
)
abstract class RelayGuardDatabase : RoomDatabase() {
    abstract fun filterDao(): FilterDao
    abstract fun messageLogDao(): MessageLogDao
}
