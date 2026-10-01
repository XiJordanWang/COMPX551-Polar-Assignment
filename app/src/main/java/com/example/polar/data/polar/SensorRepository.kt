package com.example.polar.data.polar

class SensorRepository(
    private val polarManager: PolarManager
) {

    val sensorData =
        polarManager.sensorData

}