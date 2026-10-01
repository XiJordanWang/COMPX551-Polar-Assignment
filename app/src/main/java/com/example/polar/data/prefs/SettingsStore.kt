package com.example.polar.data.prefs

/**
 * Manages user privacy mode and consent settings using Jetpack DataStore Preferences.
 */

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class PrivacyMode {
    FULL, SHARE, READ_ONLY
}

private val Context.dataStore by preferencesDataStore(name = "user_settings")

object SettingsStore {

    // Returns flow of PrivacyMode for the given username
    fun privacyMode(context: Context, username: String): Flow<PrivacyMode> {
        val key = stringPreferencesKey("mode_$username")
        return context.dataStore.data.map { prefs ->
            val value = prefs[key] ?: PrivacyMode.FULL.name
            try {
                PrivacyMode.valueOf(value)
            } catch (e: Exception) {
                PrivacyMode.FULL
            }
        }
    }

    // Updates PrivacyMode for the given username
    suspend fun setPrivacyMode(context: Context, username: String, mode: PrivacyMode) {
        val key = stringPreferencesKey("mode_$username")
        context.dataStore.edit { prefs ->
            prefs[key] = mode.name
        }
    }

    // Returns flow of consent version for the given username
    fun consentVersion(context: Context, username: String): Flow<Int> {
        val key = intPreferencesKey("consent_version_$username")
        return context.dataStore.data.map { prefs ->
            prefs[key] ?: 0
        }
    }

    // Returns flow of consent accepted timestamp for the given username
    fun consentTime(context: Context, username: String): Flow<Long> {
        val key = stringPreferencesKey("consent_time_$username")
        return context.dataStore.data.map { prefs ->
            prefs[key]?.toLongOrNull() ?: 0L
        }
    }

    // Sets consent version and saves acceptance timestamp for the given username
    suspend fun setConsent(context: Context, username: String, version: Int) {
        val versionKey = intPreferencesKey("consent_version_$username")
        val timeKey = stringPreferencesKey("consent_time_$username")
        val now = System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            prefs[versionKey] = version
            prefs[timeKey] = now.toString()
        }
    }

    // Clears all DataStore settings keys for the specified user
    suspend fun clearUserData(context: Context, username: String) {
        val modeKey = stringPreferencesKey("mode_$username")
        val versionKey = intPreferencesKey("consent_version_$username")
        val timeKey = stringPreferencesKey("consent_time_$username")
        context.dataStore.edit { prefs ->
            prefs.remove(modeKey)
            prefs.remove(versionKey)
            prefs.remove(timeKey)
        }
    }
}
