package com.ranjanvipul.relayguard.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

data class WebhookPayload(
    val message: String,
    val filterId: String,
    val recipientLabel: String
)

interface RelayApi {
    @POST
    suspend fun postWebhook(@Url url: String, @Body payload: WebhookPayload): Response<Unit>
}
