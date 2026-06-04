package com.ranjanvipul.relayguard.data.transport

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryConfigTest {
    private val gson = Gson()

    @Test
    fun smtpConfigRoundTripsFromRecipientAddress() {
        val config = SmtpEmailConfig(
            host = "smtp.example.test",
            port = 587,
            security = SmtpSecurity.StartTls,
            username = "sender@example.test",
            fromAddress = "sender@example.test",
            toAddress = "inbox@example.test"
        )

        val restored = gson.fromJson(gson.toJson(config), SmtpEmailConfig::class.java)

        assertEquals(config, restored)
    }

    @Test
    fun whatsappConfigRoundTripsFromRecipientAddress() {
        val config = WhatsAppBusinessConfig(
            messagesEndpoint = "https://graph.facebook.com/v20.0/123/messages",
            toPhoneNumber = "15551234567"
        )

        val restored = gson.fromJson(gson.toJson(config), WhatsAppBusinessConfig::class.java)

        assertEquals(config, restored)
    }
}
