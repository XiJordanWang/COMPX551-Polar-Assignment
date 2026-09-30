package com.example.polar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Which Polar H10 each user connects to on this phone (local only, Room).
@Entity(tableName = "devices")
data class Device(@PrimaryKey val username: String,
                  val deviceId: String)   // 8 characters, e.g. "B5E1A12F"
