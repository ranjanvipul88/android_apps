package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.EncryptionHelper
import com.example.security.SmsSecurityAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun verifyAppNameInContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SMS Backup & Safe Forward", appName)
    }

    @Test
    fun testEncryptionRoundTrip() {
        val originalText = "This is a secret SMS message body to encrypt."
        val encryptedText = EncryptionHelper.encrypt(originalText)
        
        // Assert that ciphertext does not contain plaintext
        assertFalse(encryptedText.contains("secret"))
        
        val decryptedText = EncryptionHelper.decrypt(encryptedText)
        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testSecurityAnalyzerExclusions() {
        // Test bank alerts
        val bankAlert = "BANK: You spent $45.20 at Amazon. Ref: txId 98734."
        val bankCheck = SmsSecurityAnalyzer.analyze(bankAlert)
        assertFalse("Bank transaction must be blocked", bankCheck.isSafe)
        assertTrue(bankCheck.matchedPattern.contains("bank", ignoreCase = true) || bankCheck.matchedPattern.contains("transaction", ignoreCase = true) || bankCheck.matchedPattern.contains("Financial", ignoreCase = true))

        // Test classic OTP
        val otpAlert = "Dear Customer, 982345 is your one time login verification PIN. Do not share."
        val otpCheck = SmsSecurityAnalyzer.analyze(otpAlert)
        assertFalse("One-time security passwords must be blocked", otpCheck.isSafe)
        assertTrue(otpCheck.matchedPattern.contains("otp", ignoreCase = true) || otpCheck.matchedPattern.contains("pattern", ignoreCase = true) || otpCheck.matchedPattern.contains("PIN", ignoreCase = true))

        // Test short standalone login validation numbers (e.g. 5 digits)
        val shortcode = "Your auth code is 54932"
        val codeCheck = SmsSecurityAnalyzer.analyze(shortcode)
        assertFalse("Short numbers often indicating login auth pins must be blocked", codeCheck.isSafe)

        // Test normal friendly sms
        val safeSms = "Hey there! Are we still on for lunch today at the corner cafe at 1 PM?"
        val safeCheck = SmsSecurityAnalyzer.analyze(safeSms)
        assertTrue("Normal conversations without risk patterns must be safe", safeCheck.isSafe)
    }
}
