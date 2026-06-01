package com.example.whatsappwebnative

import android.content.Context

object AccountNameStore {
    private const val PREFS = "account_names"
    private const val KEY_PREFIX = "account_name_"

    fun getName(context: Context, slot: AccountSlot): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PREFIX + slot.number, null)
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.default_account_name, slot.number)
    }

    fun setName(context: Context, slot: AccountSlot, name: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PREFIX + slot.number, name.trim())
            .apply()
    }
}
