package com.example.polar.data.polar

import android.content.Context
import android.util.Log
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.PolarDeviceInfo
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PolarManager(context: Context) {

    private val api: PolarBleApi =
        PolarBleApiDefaultImpl.defaultImplementation(
            context,
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE
            )
        )

    private val _sensorData = MutableStateFlow(SensorData())
    val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()

    private var hrDisposable: Disposable? = null
    private var accDisposable: Disposable? = null

    init {
        api.setApiCallback(object : PolarBleApiCallback() {

            override fun deviceConnecting(
                polarDeviceInfo: PolarDeviceInfo
            ) {
                Log.d(
                    "POLAR",
                    "CONNECTING ${polarDeviceInfo.deviceId}"
                )
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("POLAR", "CONNECTED ${polarDeviceInfo.deviceId}")
                _sensorData.value = _sensorData.value.copy(
                    connected = true,
                    deviceId = polarDeviceInfo.deviceId
                )
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("POLAR", "DISCONNECTED ${polarDeviceInfo.deviceId}")
                cleanupStreams()
                _sensorData.value = _sensorData.value.copy(
                    connected = false
                )
            }

            override fun bleSdkFeatureReady(
                identifier: String,
                feature: PolarBleApi.PolarBleSdkFeature
            ) {
                Log.d("POLAR", "FEATURE READY: $feature on $identifier")

                if (feature == PolarBleApi.PolarBleSdkFeature.FEATURE_HR) {
                    startHrStreaming(identifier)
                }

                if (feature == PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING ||
                    feature == PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE
                ) {
                    startAccStreaming(identifier)
                }
            }
        })
    }

    private fun startHrStreaming(deviceId: String) {
        hrDisposable?.dispose()
        hrDisposable = api.startHrStreaming(deviceId)
            .subscribe(
                { polarHrData ->
                    val hr = polarHrData.samples.lastOrNull()?.hr ?: 0
                    if (hr > 0) {
                        _sensorData.value = _sensorData.value.copy(heartRate = hr)
                    }
                },
                { error ->
                    Log.e("POLAR", "Error streaming HR: ${error.message}", error)
                }
            )
    }

    private fun startAccStreaming(deviceId: String) {
        accDisposable?.dispose()
        accDisposable = api.requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ACC)
            .flatMapPublisher { settings ->
                api.startAccStreaming(deviceId, settings)
            }
            .subscribe(
                { polarAccData ->
                    val lastSample = polarAccData.samples.lastOrNull()
                    if (lastSample != null) {
                        _sensorData.value = _sensorData.value.copy(
                            accX = lastSample.x.toFloat(),
                            accY = lastSample.y.toFloat(),
                            accZ = lastSample.z.toFloat()
                        )
                    }
                },
                { error ->
                    Log.e("POLAR", "Error streaming ACC: ${error.message}", error)
                }
            )
    }

    fun connect(deviceId: String) {
        Log.d("POLAR", "connect() called for $deviceId")
        try {
            api.connectToDevice(deviceId)
            Log.d("POLAR", "connectToDevice() executed")
        } catch (e: Exception) {
            Log.e("POLAR", "Connection error: ${e.message}", e)
        }
    }

    fun disconnect(deviceId: String? = null) {
        cleanupStreams()
        try {
            if (deviceId != null) {
                api.disconnectFromDevice(deviceId)
            } else {
                val currentId = _sensorData.value.deviceId
                if (currentId.isNotEmpty()) {
                    api.disconnectFromDevice(currentId)
                }
            }
        } catch (e: Exception) {
            Log.e("POLAR", "Disconnect error: ${e.message}", e)
        }
    }

    private fun cleanupStreams() {
        hrDisposable?.dispose()
        hrDisposable = null
        accDisposable?.dispose()
        accDisposable = null
    }
}