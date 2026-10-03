package com.example.polar.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.polar.logic.CoachMode
import com.example.polar.logic.DEFAULT_STYLE_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Saves each user's settings on this phone with DataStore: privacy mode, coach mode, plant style and consent. */
// FULL = "Saved locally", SHARE = "Uploaded to cloud"
enum class PrivacyMode {
    FULL, SHARE
}

val PrivacyMode.title: String
    get() = when (this) {
        PrivacyMode.FULL -> "Saved locally"
        PrivacyMode.SHARE -> "Uploaded to cloud"
    }

val PrivacyMode.description: String
    get() = when (this) {
        PrivacyMode.FULL -> "Data is saved only on your phone. You won't appear on the leaderboard."
        PrivacyMode.SHARE -> "Workout summaries are uploaded to Supabase so you can participate in the leaderboard."
    }

private val Context.dataStore by preferencesDataStore(name = "user_settings")

object SettingsStore {

    // The user's privacy mode. Defaults to "Saved locally" if they haven't picked one.
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

    // Saves the user's privacy mode
    suspend fun setPrivacyMode(context: Context, username: String, mode: PrivacyMode) {
        val key = stringPreferencesKey("mode_$username")
        context.dataStore.edit { prefs ->
            prefs[key] = mode.name
        }
    }

    // The user's coach personality (default SUPPORTIVE)
    fun coachMode(context: Context, username: String): Flow<CoachMode> {
        val key = stringPreferencesKey("coach_mode_$username")
        return context.dataStore.data.map { prefs ->
            val value = prefs[key] ?: CoachMode.SUPPORTIVE.name
            try {
                CoachMode.valueOf(value)
            } catch (e: Exception) {
                CoachMode.SUPPORTIVE
            }
        }
    }

    // Saves the user's coach personality
    suspend fun setCoachMode(context: Context, username: String, mode: CoachMode) {
        val key = stringPreferencesKey("coach_mode_$username")
        context.dataStore.edit { prefs ->
            prefs[key] = mode.name
        }
    }

    // The plant style the user picked in the shop (default "classic")
    fun plantStyle(context: Context, username: String): Flow<String> {
        val key = stringPreferencesKey("plant_style_$username")
        return context.dataStore.data.map { prefs ->
            prefs[key] ?: DEFAULT_STYLE_ID
        }
    }

    // Saves the user's plant style
    suspend fun setPlantStyle(context: Context, username: String, styleId: String) {
        val key = stringPreferencesKey("plant_style_$username")
        context.dataStore.edit { prefs ->
            prefs[key] = styleId
        }
    }

    // Which version of the consent screen the user agreed to (0 = not yet)
    fun consentVersion(context: Context, username: String): Flow<Int> {
        val key = intPreferencesKey("consent_version_$username")
        return context.dataStore.data.map { prefs ->
            prefs[key] ?: 0
        }
    }

    // When the user agreed to the consent screen, in milliseconds (0 = not yet)
    fun consentTime(context: Context, username: String): Flow<Long> {
        val key = stringPreferencesKey("consent_time_$username")
        return context.dataStore.data.map { prefs ->
            prefs[key]?.toLongOrNull() ?: 0L
        }
    }

    // Saves the consent version the user agreed to, and the time they agreed
    suspend fun setConsent(context: Context, username: String, version: Int) {
        val versionKey = intPreferencesKey("consent_version_$username")
        val timeKey = stringPreferencesKey("consent_time_$username")
        val now = System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            prefs[versionKey] = version
            prefs[timeKey] = now.toString()
        }
    }

    // Removes all of this user's settings (used by "Delete my data")
    suspend fun clearUserData(context: Context, username: String) {
        val modeKey = stringPreferencesKey("mode_$username")
        val versionKey = intPreferencesKey("consent_version_$username")
        val timeKey = stringPreferencesKey("consent_time_$username")
        val coachKey = stringPreferencesKey("coach_mode_$username")
        val styleKey = stringPreferencesKey("plant_style_$username")
        context.dataStore.edit { prefs ->
            prefs.remove(modeKey)
            prefs.remove(versionKey)
            prefs.remove(timeKey)
            prefs.remove(coachKey)
            prefs.remove(styleKey)
        }
    }
}