package com.example.whatsappwebnative

import android.content.Context
import java.security.MessageDigest

object ScreenLockStore {
    private const val PREFS = "screen_lock"
    private const val KEY_PIN_HASH = "pin_hash"

    var sessionUnlocked: Boolean = false

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .contains(KEY_PIN_HASH)
    }

    fun setPin(context: Context, pin: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PIN_HASH, hash(pin))
            .apply()
        sessionUnlocked = true
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PIN_HASH)
            .apply()
        sessionUnlocked = true
    }

    fun verify(context: Context, pin: String): Boolean {
        val expected = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PIN_HASH, null)
            ?: return true
        val valid = expected == hash(pin)
        if (valid) sessionUnlocked = true
        return valid
    }

    fun lockNow() {
        sessionUnlocked = false
    }

    private fun hash(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
