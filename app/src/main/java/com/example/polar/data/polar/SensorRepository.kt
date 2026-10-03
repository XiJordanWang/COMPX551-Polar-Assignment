package com.example.polar.data.polar

// Repository wrapper that exposes sensor data from PolarManager.
// This decouples UI screens from the underlying Polar SDK logic.
class SensorRepository(
    // Reference to the active PolarManager instance
    private val polarManager: PolarManager
) {

    // Exposes the live sensor data stream to UI components
    val sensorData =
        polarManager.sensorData

}
