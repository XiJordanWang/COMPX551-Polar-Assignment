package com.example.polar.data.polar

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PolarManager {

    private val _sensorData = MutableStateFlow(
        SensorData()
    )

    val sensorData: StateFlow<SensorData> =
        _sensorData.asStateFlow()

    fun connect(deviceId: String) {
        // Polar SDK connection will go here later
    }

    fun disconnect() {
        // Disconnect logic will go here later
    }

    fun updateHeartRate(heartRate: Int) {
        _sensorData.value = _sensorData.value.copy(
            heartRate = heartRate
        )
    }

    fun updateAccelerometer(
        x: Float,
        y: Float,
        z: Float
    ) {
        _sensorData.value = _sensorData.value.copy(
            accX = x,
            accY = y,
            accZ = z
        )
    }

    fun updateConnectionState(
        connected: Boolean
    ) {
        _sensorData.value = _sensorData.value.copy(
            connected = connected
        )
    }
}