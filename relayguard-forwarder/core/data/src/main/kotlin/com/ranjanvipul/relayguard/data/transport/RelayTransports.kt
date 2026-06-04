package com.ranjanvipul.relayguard.data.transport

import android.telephony.SmsManager
import com.ranjanvipul.relayguard.data.network.RelayApi
import com.ranjanvipul.relayguard.data.network.WebhookPayload
import com.ranjanvipul.relayguard.data.security.SecretStore
import com.ranjanvipul.relayguard.domain.model.RecipientKind
import com.ranjanvipul.relayguard.domain.model.RenderedRelay
import com.ranjanvipul.relayguard.domain.repository.RelayTransport

class CompositeRelayTransport(
    private val sms: SmsRelayTransport,
    private val webhook: WebhookRelayTransport,
    private val email: StubRelayTransport,
    private val chat: ChatWebhookRelayTransport
) : RelayTransport {
    override suspend fun send(relay: RenderedRelay): Result<Unit> = when (relay.recipient.kind) {
        RecipientKind.SmsNumber -> sms.send(relay)
        RecipientKind.Webhook -> webhook.send(relay)
        RecipientKind.Email -> email.send(relay)
        RecipientKind.Telegram, RecipientKind.Slack -> chat.send(relay)
    }
}

class SmsRelayTransport(private val smsManagerFactory: () -> SmsManager) : RelayTransport {
    override suspend fun send(relay: RenderedRelay): Result<Unit> = runCatching {
        val parts = smsManagerFactory().divideMessage(relay.body)
        smsManagerFactory().sendMultipartTextMessage(relay.recipient.address, null, parts, null, null)
    }
}

class WebhookRelayTransport(private val api: RelayApi) : RelayTransport {
    override suspend fun send(relay: RenderedRelay): Result<Unit> = runCatching {
        val response = api.postWebhook(
            relay.recipient.address,
            WebhookPayload(relay.body, relay.filterId, relay.recipient.label)
        )
        check(response.isSuccessful) { "Webhook returned HTTP ${response.code()}" }
    }
}

class ChatWebhookRelayTransport(
    private val api: RelayApi,
    private val secretStore: SecretStore
) : RelayTransport {
    override suspend fun send(relay: RenderedRelay): Result<Unit> = runCatching {
        val secret = relay.recipient.secretAlias?.let(secretStore::get)
        val url = relay.recipient.address.replace("{{secret}}", secret.orEmpty())
        val response = api.postWebhook(url, WebhookPayload(relay.body, relay.filterId, relay.recipient.label))
        check(response.isSuccessful) { "Chat webhook returned HTTP ${response.code()}" }
    }
}

class StubRelayTransport(private val label: String) : RelayTransport {
    override suspend fun send(relay: RenderedRelay): Result<Unit> =
        Result.failure(UnsupportedOperationException("$label requires account setup before sending."))
}
