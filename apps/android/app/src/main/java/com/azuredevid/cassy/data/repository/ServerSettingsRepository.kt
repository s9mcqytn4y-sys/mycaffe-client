package com.azuredevid.cassy.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.azuredevid.cassy.BuildConfig

interface ServerSettingsRepository {
    fun getBaseUrl(): String
    fun setBaseUrl(url: String)
}

class SharedPreferencesServerSettingsRepository(context: Context) : ServerSettingsRepository {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getBaseUrl(): String {
        val raw = prefs.getString(KEY_BASE_URL, BuildConfig.BASE_API_URL) ?: BuildConfig.BASE_API_URL
        return normalizeBaseUrl(raw)
    }

    override fun setBaseUrl(url: String) {
        val normalized = normalizeBaseUrl(url)
        if (normalized.isNotBlank()) {
            prefs.edit().putString(KEY_BASE_URL, normalized).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "cassy_server_settings"
        private const val KEY_BASE_URL = "server_base_url"

        fun normalizeBaseUrl(raw: String): String {
            return raw.trim()
                .trimEnd('/')
                .removeSuffix("/api/v1")
                .trimEnd('/')
                .ifBlank { BuildConfig.BASE_API_URL }
        }
    }
}
