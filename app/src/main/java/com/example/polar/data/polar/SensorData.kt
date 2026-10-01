package com.example.polar.data.polar

data class SensorData(
    val heartRate: Int = 0,
    val accX: Float = 0f,
    val accY: Float = 0f,
    val accZ: Float = 0f,
    val connected: Boolean = false,
    val deviceId: String = ""
)