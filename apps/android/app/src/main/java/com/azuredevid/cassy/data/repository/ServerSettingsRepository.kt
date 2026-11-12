package com.azuredevid.cassy.data.repository

import android.content.Context
import android.content.SharedPreferences

/**
 * Repository interface and default SharedPreferences implementation for local server settings
 * and supervisor authorization policy.
 */
interface ServerSettingsRepository {
    fun isSupervisorPinRequired(): Boolean
    fun verifySupervisorPin(pin: String): Boolean
    fun setSupervisorPin(pin: String)
    fun getBaseUrl(): String
    fun setBaseUrl(url: String)
}

class SharedPreferencesServerSettingsRepository(context: Context) : ServerSettingsRepository {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cassy_server_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SUPERVISOR_PIN = "supervisor_pin"
        private const val KEY_BASE_URL = "server_base_url"
        private const val DEFAULT_SUPERVISOR_PIN = "1234"
        private const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"
    }

    override fun isSupervisorPinRequired(): Boolean = true

    override fun verifySupervisorPin(pin: String): Boolean {
        val storedPin = prefs.getString(KEY_SUPERVISOR_PIN, DEFAULT_SUPERVISOR_PIN) ?: DEFAULT_SUPERVISOR_PIN
        return pin.trim() == storedPin.trim()
    }

    override fun setSupervisorPin(pin: String) {
        prefs.edit().putString(KEY_SUPERVISOR_PIN, pin).apply()
    }

    override fun getBaseUrl(): String {
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    override fun setBaseUrl(url: String) {
        prefs.edit().putString(KEY_BASE_URL, url).apply()
    }
}

