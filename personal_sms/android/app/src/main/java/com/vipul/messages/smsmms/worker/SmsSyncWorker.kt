package com.vipul.messages.smsmms.worker

import android.content.Context
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vipul.messages.smsmms.data.SmsDatabase
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SmsSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = "SmsSyncWorker"
    private val database = SmsDatabase.getDatabase(appContext)
    private val sharedPrefs = appContext.getSharedPreferences("sms_replica_prefs", Context.MODE_PRIVATE)

    // Set up Ktor Client with JSON parser
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    override suspend fun doWork(): Result {
        val serverUrl = sharedPrefs.getString("sync_server", "http://localhost:3000") ?: "http://localhost:3000"
        val deviceToken = sharedPrefs.getString("device_token", "") ?: ""

        if (deviceToken.isEmpty()) {
            Log.w(TAG, "Sync aborted. Device not paired with Web Portal.")
            return Result.success()
        }

        try {
            // 1. Fetch local unsynced messages
            val unsyncedList = database.smsDao().getUnsyncedMessages()
            Log.d(TAG, "Found ${unsyncedList.size} unsynced messages.")

            // 2. Fetch battery status telemetry
            val batteryLevel = getBatteryLevel(applicationContext)

            // 3. Prepare serialized sync package
            val payload = SyncPayload(
                battery_level = batteryLevel,
                network_signal = "Strong", // Dynamic evaluation via TelephonyManager in prod
                messages = unsyncedList.map {
                    NetworkSms(
                        message_id = it.message_id,
                        phone_number = it.phone_number,
                        sender_name = it.sender_name,
                        body = it.body,
                        timestamp = it.timestamp,
                        direction = it.direction,
                        type = it.type,
                        mms_attachment_url = it.mms_attachment_url,
                        is_starred = it.is_starred
                    )
                }
            )

            // 4. Synchronize via REST API
            val response = client.post("$serverUrl/api/sync/sms") {
                contentType(ContentType.Application.Json)
                header("x-device-token", deviceToken)
                setBody(payload)
            }

            if (response.status == HttpStatusCode.OK) {
                val responseBody = response.bodyAsText()
                val syncResult = Json.decodeFromString<SyncResponse>(responseBody)

                // 5. Update local database markers
                unsyncedList.forEach { msg ->
                    database.smsDao().markSynced(msg.message_id)
                }
                Log.d(TAG, "Successfully synced ${unsyncedList.size} messages.")

                // 6. Update local Forwarding settings sent by the Web Portal controls
                sharedPrefs.edit().apply {
                    putBoolean("forward_enabled", syncResult.is_forward_enabled)
                    putString("forward_phone", syncResult.forward_number)
                    apply()
                }
                Log.d(TAG, "Updated local forwarding setups from Web Dashboard settings: ${syncResult.is_forward_enabled} | ${syncResult.forward_number}")

                return Result.success()
            } else {
                Log.e(TAG, "Server responded with failure: ${response.status}")
                return Result.retry()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Background execution sync failure", e)
            return Result.retry()
        } finally {
            client.close()
        }
    }

    private fun getBatteryLevel(context: Context): Int {
        val batteryStatus = context.registerReceiver(null, IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale) else 100
    }
}

// --- Serialized Network Transfer Schemas ---

@Serializable
data class SyncPayload(
    val battery_level: Int,
    val network_signal: String,
    val messages: List<NetworkSms>
)

@Serializable
data class NetworkSms(
    val message_id: String,
    val phone_number: String,
    val sender_name: String,
    val body: String,
    val timestamp: Long,
    val direction: String,
    val type: String,
    val mms_attachment_url: String,
    val is_starred: Boolean
)

@Serializable
data class SyncResponse(
    val status: String,
    val synced: Int,
    val forward_number: String,
    val is_forward_enabled: Boolean
)
