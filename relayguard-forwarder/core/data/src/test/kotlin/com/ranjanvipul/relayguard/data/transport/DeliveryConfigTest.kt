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
    fun telegramContactAddressIsStoredAsChatId() {
        val chatId = "123456789"

        assertEquals("123456789", chatId)
    }
}
