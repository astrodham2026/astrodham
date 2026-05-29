package com.astrodham.astroagent.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages the Codemax.pro API key using SharedPreferences.
 * Stores the key on-device so users can enter it from the app UI
 * without needing to rebuild.
 */
object ApiKeyManager {

    private const val PREFS_NAME = "astro_agent_prefs"
    private const val KEY_API_KEY = "codemax_api_key"
    private const val KEY_BASE_URL = "codemax_base_url"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        try {
            // Try encrypted SharedPreferences first
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            prefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences if encryption fails
            Logger.w("Encrypted prefs unavailable, using standard prefs: ${e.message}")
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Saves the API key.
     */
    fun saveApiKey(key: String) {
        prefs?.edit()?.putString(KEY_API_KEY, key.trim())?.apply()
        Logger.i("API key saved (${key.take(10)}...)")
    }

    /**
     * Retrieves the stored API key.
     */
    fun getApiKey(): String {
        return prefs?.getString(KEY_API_KEY, "") ?: ""
    }

    /**
     * Checks if an API key is configured.
     */
    fun hasApiKey(): Boolean {
        return getApiKey().isNotBlank()
    }

    /**
     * Saves a custom base URL (optional override).
     */
    fun saveBaseUrl(url: String) {
        prefs?.edit()?.putString(KEY_BASE_URL, url.trim())?.apply()
    }

    /**
     * Gets the base URL (returns default if not overridden).
     */
    fun getBaseUrl(): String {
        val custom = prefs?.getString(KEY_BASE_URL, "") ?: ""
        return if (custom.isNotBlank()) custom else Constants.CODEMAX_BASE_URL
    }

    /**
     * Clears the stored API key.
     */
    fun clearApiKey() {
        prefs?.edit()?.remove(KEY_API_KEY)?.apply()
        Logger.i("API key cleared")
    }
}
