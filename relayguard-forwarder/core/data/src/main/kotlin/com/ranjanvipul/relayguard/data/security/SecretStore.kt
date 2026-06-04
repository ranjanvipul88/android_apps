package com.ranjanvipul.relayguard.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface SecretStore {
    fun put(alias: String, value: String)
    fun get(alias: String): String?
    fun remove(alias: String)
}

class AndroidSecretStore(context: Context) : SecretStore {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        "relayguard_secrets",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun put(alias: String, value: String) {
        preferences.edit().putString(alias, value).apply()
    }

    override fun get(alias: String): String? = preferences.getString(alias, null)

    override fun remove(alias: String) {
        preferences.edit().remove(alias).apply()
    }
}
