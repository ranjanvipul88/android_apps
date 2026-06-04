package com.ranjanvipul.relayguard.data.transport

import com.google.gson.Gson
import android.telephony.SmsManager
import com.ranjanvipul.relayguard.data.network.RelayApi
import com.ranjanvipul.relayguard.data.network.WebhookPayload
import com.ranjanvipul.relayguard.data.security.SecretStore
import com.ranjanvipul.relayguard.domain.model.RecipientKind
import com.ranjanvipul.relayguard.domain.model.RenderedRelay
import com.ranjanvipul.relayguard.domain.repository.RelayTransport
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class CompositeRelayTransport(
    private val sms: SmsRelayTransport,
    private val webhook: WebhookRelayTransport,
    private val email: EmailRelayTransport,
    private val telegram: TelegramRelayTransport,
    private val slack: SlackRelayTransport
) : RelayTransport {
    override suspend fun send(relay: RenderedRelay): Result<Unit> = when (relay.recipient.kind) {
        RecipientKind.SmsNumber -> sms.send(relay)
        RecipientKind.Webhook -> webhook.send(relay)
        RecipientKind.Email -> email.send(relay)
        RecipientKind.Telegram -> telegram.send(relay)
        RecipientKind.Slack -> slack.send(relay)
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

class TelegramRelayTransport(
    private val client: OkHttpClient,
    private val secretStore: SecretStore,
    private val gson: Gson = Gson()
) : RelayTransport {
    override suspend fun send(relay: RenderedRelay): Result<Unit> = runCatching {
        val token = relay.recipient.secretAlias?.let(secretStore::get).orEmpty()
        val chatId = relay.recipient.address
        require(token.isNotBlank()) { "Telegram bot token is required." }
        require(chatId.isNotBlank()) { "Telegram chat id is required." }
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://api.telegram.org/bot$token/sendMessage")
                .post(
                    gson.toJson(mapOf("chat_id" to chatId, "text" to relay.body))
                        .toRequestBody("application/json".toMediaType())
                )
                .build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Telegram returned HTTP ${response.code}" }
            }
        }
    }
}

class SlackRelayTransport(
    private val client: OkHttpClient,
    private val gson: Gson = Gson()
) : RelayTransport {
    override suspend fun send(relay: RenderedRelay): Result<Unit> = runCatching {
        require(relay.recipient.address.startsWith("https://")) { "Slack webhook must use HTTPS." }
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(relay.recipient.address)
                .post(gson.toJson(mapOf("text" to relay.body)).toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Slack returned HTTP ${response.code}" }
            }
        }
    }
}

enum class SmtpSecurity {
    StartTls,
    SslTls
}

data class SmtpEmailConfig(
    val host: String,
    val port: Int,
    val security: SmtpSecurity,
    val username: String,
    val fromAddress: String,
    val toAddress: String
)

class EmailRelayTransport(
    private val secretStore: SecretStore,
    private val gson: Gson = Gson()
) : RelayTransport {
    override suspend fun send(relay: RenderedRelay): Result<Unit> = runCatching {
        val config = gson.fromJson(relay.recipient.address, SmtpEmailConfig::class.java)
        val password = relay.recipient.secretAlias?.let(secretStore::get).orEmpty()
        require(config.host.isNotBlank()) { "SMTP host is required." }
        require(config.toAddress.isNotBlank()) { "Email recipient is required." }
        require(config.fromAddress.isNotBlank()) { "Email sender is required." }
        withContext(Dispatchers.IO) {
            SmtpClient(config, password).send(
                subject = "Forwarded SMS from RelayGuard",
                body = relay.body
            )
        }
    }
}

private class SmtpClient(
    private val config: SmtpEmailConfig,
    private val password: String
) {
    fun send(subject: String, body: String) {
        val socket = when (config.security) {
            SmtpSecurity.SslTls -> sslSocket()
            SmtpSecurity.StartTls -> plainSocket()
        }
        socket.use { initialSocket ->
            val session = SmtpSession(initialSocket)
            session.expect(220)
            session.command("EHLO relayguard.local", 250)
            val activeSession = if (config.security == SmtpSecurity.StartTls) {
                session.command("STARTTLS", 220)
                val tlsSocket = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                    .createSocket(initialSocket, config.host, config.port, true) as SSLSocket
                tlsSocket.startHandshake()
                val tlsSession = SmtpSession(tlsSocket)
                tlsSession.command("EHLO relayguard.local", 250)
                tlsSession
            } else {
                session
            }
            if (config.username.isNotBlank()) {
                activeSession.command("AUTH LOGIN", 334)
                activeSession.command(config.username.base64(), 334)
                activeSession.command(password.base64(), 235)
            }
            activeSession.command("MAIL FROM:<${config.fromAddress.sanitizedAddress()}>", 250)
            activeSession.command("RCPT TO:<${config.toAddress.sanitizedAddress()}>", 250, 251)
            activeSession.command("DATA", 354)
            activeSession.writeData(buildMessage(subject, body))
            activeSession.expect(250)
            activeSession.command("QUIT", 221)
        }
    }

    private fun plainSocket(): Socket = Socket().apply {
        connect(InetSocketAddress(config.host, config.port), 20_000)
        soTimeout = 20_000
    }

    private fun sslSocket(): Socket =
        (SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(config.host, config.port).apply {
            soTimeout = 20_000
        }

    private fun buildMessage(subject: String, body: String): String {
        val safeSubject = subject.sanitizedHeader()
        val date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now())
        val lines = body.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val escapedBody = lines.joinToString("\r\n") { if (it.startsWith(".")) ".$it" else it }
        return buildString {
            append("From: <${config.fromAddress.sanitizedAddress()}>\r\n")
            append("To: <${config.toAddress.sanitizedAddress()}>\r\n")
            append("Subject: $safeSubject\r\n")
            append("Date: $date\r\n")
            append("MIME-Version: 1.0\r\n")
            append("Content-Type: text/plain; charset=UTF-8\r\n")
            append("Content-Transfer-Encoding: 8bit\r\n")
            append("\r\n")
            append(escapedBody)
            append("\r\n.")
        }
    }

    private fun String.base64(): String =
        java.util.Base64.getEncoder().encodeToString(toByteArray(Charsets.UTF_8))

    private fun String.sanitizedHeader(): String = replace(Regex("[\\r\\n]"), " ").trim()

    private fun String.sanitizedAddress(): String = replace(Regex("[\\r\\n<>]"), "").trim()
}

private class SmtpSession(socket: Socket) {
    private val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
    private val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))

    fun command(command: String, vararg expected: Int) {
        writer.write("$command\r\n")
        writer.flush()
        expect(*expected)
    }

    fun writeData(data: String) {
        writer.write("$data\r\n")
        writer.flush()
    }

    fun expect(vararg expected: Int) {
        val lines = readResponseLines()
        val code = lines.firstOrNull()?.take(3)?.toIntOrNull()
        check(code in expected.toSet()) { "SMTP returned ${lines.joinToString(" | ")}" }
    }

    private fun readResponseLines(): List<String> {
        val lines = mutableListOf<String>()
        while (true) {
            val line = reader.readLine() ?: error("SMTP connection closed.")
            lines += line
            if (line.length < 4 || line[3] != '-') return lines
        }
    }
}
