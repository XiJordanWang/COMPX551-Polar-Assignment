package com.example.polar.data.polar

// Holds the latest sensor readings and connection status from the Polar H10 device.
data class SensorData(
    // Current heart rate in beats per minute (BPM)
    val heartRate: Int = 0,
    // Accelerometer reading for left/right movement
    val accX: Float = 0f,
    // Accelerometer reading for forward/backward movement
    val accY: Float = 0f,
    // Accelerometer reading for up/down movement
    val accZ: Float = 0f,
    // True if the Polar H10 sensor is currently connected via Bluetooth
    val connected: Boolean = false,
    // The unique 8-character device ID of the connected Polar H10
    val deviceId: String = ""
)
