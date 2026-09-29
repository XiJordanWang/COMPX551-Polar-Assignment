package com.example.polar.data.prefs

import android.content.Context

// Remembers which Polar H10 to connect to.
// It is one small setting for this phone, not per user, so SharedPreferences
// fits better than a Room table (and needs no database migration).
object DeviceStore {

    private const val PREFS_NAME = "device_settings"
    private const val KEY_DEVICE_ID = "polar_device_id"

    // Returns null if the user hasn't entered a device ID yet.
    // The Polar SDK code can call this before connecting.
    fun getDeviceId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEVICE_ID, null)
    }

    fun saveDeviceId(context: Context, deviceId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }
}
