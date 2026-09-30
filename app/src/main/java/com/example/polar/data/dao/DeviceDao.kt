package com.example.polar.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.polar.data.entity.Device
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    // Insert, or update if this user already has a device
    @Upsert
    suspend fun save(device: Device)

    // Get the device ID once. The Polar SDK code can call this before connecting.
    @Query("SELECT deviceId FROM devices WHERE username = :username LIMIT 1")
    suspend fun getDeviceId(username: String): String?

    // Same, but updates by itself when the device ID changes (for the screen)
    @Query("SELECT deviceId FROM devices WHERE username = :username LIMIT 1")
    fun observeDeviceId(username: String): Flow<String?>
}
