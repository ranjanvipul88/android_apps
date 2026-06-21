package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object TelegramSender {
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun sendMessage(botToken: String, chatId: String, text: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.telegram.org/bot$botToken/sendMessage"
                
                val jsonBody = JSONObject().apply {
                    put("chat_id", chatId)
                    put("text", text)
                }

                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toString().toRequestBody(JSON))
                    .build()

                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
