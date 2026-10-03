package com.example.polar.data.polar

/** Passes on PolarManager's sensor data, so screens could read it without knowing about the Polar SDK. */
class SensorRepository(
    private val polarManager: PolarManager
) {

    val sensorData =
        polarManager.sensorData

}