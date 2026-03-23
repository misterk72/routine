package com.healthtracker.jellyfin

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JellyfinSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) : JellyfinSettings {

    private val standardPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun getServerUrl(): String? = standardPrefs.getString(KEY_SERVER_URL, null)

    override fun setServerUrl(value: String?) {
        val sanitized = value?.trim()?.trimEnd('/').orEmpty()
        if (sanitized.isEmpty()) {
            standardPrefs.edit().remove(KEY_SERVER_URL).apply()
        } else {
            standardPrefs.edit().putString(KEY_SERVER_URL, sanitized).apply()
        }
    }

    override fun getUsername(): String? = standardPrefs.getString(KEY_USERNAME, null)

    override fun setUsername(value: String?) {
        val sanitized = value?.trim().orEmpty()
        if (sanitized.isEmpty()) {
            standardPrefs.edit().remove(KEY_USERNAME).apply()
        } else {
            standardPrefs.edit().putString(KEY_USERNAME, sanitized).apply()
        }
    }

    override fun getApiKey(): String? = securePrefs.getString(KEY_API_KEY, null)

    override fun setApiKey(value: String?) {
        val sanitized = value?.trim().orEmpty()
        if (sanitized.isEmpty()) {
            securePrefs.edit().remove(KEY_API_KEY).apply()
        } else {
            securePrefs.edit().putString(KEY_API_KEY, sanitized).apply()
        }
    }

    override fun isConfigured(): Boolean {
        return !getServerUrl().isNullOrBlank() &&
            !getUsername().isNullOrBlank() &&
            !getApiKey().isNullOrBlank()
    }

    private companion object {
        const val PREFS_NAME = "jellyfin_settings"
        const val SECURE_PREFS_NAME = "jellyfin_settings_secure"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_USERNAME = "username"
        const val KEY_API_KEY = "api_key"
    }
}
