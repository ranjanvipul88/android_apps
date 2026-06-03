package com.vipul.messages.smsmms.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class SmsMessage(
    @PrimaryKey
    val message_id: String,
    val phone_number: String,
    val sender_name: String,
    val body: String,
    val timestamp: Long,
    val direction: String, // "incoming" or "outgoing"
    val type: String = "sms", // "sms" or "mms"
    val mms_attachment_url: String = "",
    val is_starred: Boolean = false,
    val is_synced: Boolean = false
)
