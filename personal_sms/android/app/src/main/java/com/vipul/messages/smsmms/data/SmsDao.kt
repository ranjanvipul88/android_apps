package com.vipul.messages.smsmms.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<SmsMessage>>

    @Query("SELECT * FROM messages WHERE phone_number = :phone ORDER BY timestamp ASC")
    fun getMessagesForContact(phone: String): Flow<List<SmsMessage>>

    @Query("SELECT * FROM messages WHERE is_synced = 0")
    suspend fun getUnsyncedMessages(): List<SmsMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: SmsMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<SmsMessage>)

    @Query("UPDATE messages SET is_synced = 1 WHERE message_id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE messages SET is_starred = :starred WHERE message_id = :id")
    suspend fun toggleStar(id: String, starred: Boolean)

    @Query("DELETE FROM messages WHERE message_id = :id")
    suspend fun deleteMessage(id: String)
}
