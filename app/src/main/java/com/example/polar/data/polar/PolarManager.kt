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

// Manages Bluetooth Low Energy (BLE) connection and data streaming from the Polar H10 heart rate sensor.
class PolarManager(context: Context) {

    // Main API object provided by the Polar BLE SDK to talk to the sensor
    private val api: PolarBleApi =
        PolarBleApiDefaultImpl.defaultImplementation(
            context,
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE
            )
        )

    // Private mutable flow that holds the current sensor state
    private val _sensorData = MutableStateFlow(SensorData())
    // Public read-only flow that UI screens collect to get live sensor updates
    val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()

    // Holds the subscription to the live heart rate stream
    private var hrDisposable: Disposable? = null
    // Holds the subscription to the accelerometer data stream
    private var accDisposable: Disposable? = null

    init {
        // Enable automatic reconnection if the Bluetooth connection drops
        try {
            api.setAutomaticReconnection(true)
        } catch (e: Exception) {
            Log.e("POLAR", "Error enabling automatic reconnection: ${e.message}")
        }

        // Set up event callbacks for connection status and SDK features
        api.setApiCallback(object : PolarBleApiCallback() {

            // Triggered when the phone is attempting to connect to the sensor
            override fun deviceConnecting(
                polarDeviceInfo: PolarDeviceInfo
            ) {
                Log.d(
                    "POLAR",
                    "CONNECTING ${polarDeviceInfo.deviceId}"
                )
            }

            // Triggered when successfully connected to the Polar H10 device
            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("POLAR", "CONNECTED ${polarDeviceInfo.deviceId}")
                // Update state: mark device as connected and record its ID
                _sensorData.value = _sensorData.value.copy(
                    connected = true,
                    deviceId = polarDeviceInfo.deviceId
                )
            }

            // Triggered when the Polar H10 device disconnects
            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d("POLAR", "DISCONNECTED ${polarDeviceInfo.deviceId}")
                // Clean up active RxJava data streams
                cleanupStreams()
                // Update state: mark device as disconnected
                _sensorData.value = _sensorData.value.copy(
                    connected = false
                )
            }

            // Triggered when a specific feature (like HR or streaming) is ready to be used
            override fun bleSdkFeatureReady(
                identifier: String,
                feature: PolarBleApi.PolarBleSdkFeature
            ) {
                Log.d("POLAR", "FEATURE READY: $feature on $identifier")

                // Start heart rate streaming when HR feature is ready
                if (feature == PolarBleApi.PolarBleSdkFeature.FEATURE_HR) {
                    startHrStreaming(identifier)
                }

                // Start accelerometer streaming when streaming features are ready
                if (feature == PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING ||
                    feature == PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE
                ) {
                    startAccStreaming(identifier)
                }
            }
        })
    }

    // Subscribes to live heart rate updates from the Polar device
    private fun startHrStreaming(deviceId: String) {
        // Stop any existing HR subscription
        hrDisposable?.dispose()
        // Subscribe to incoming heart rate data packets
        hrDisposable = api.startHrStreaming(deviceId)
            .subscribe(
                { polarHrData ->
                    // Get the latest heart rate sample from the packet
                    val hr = polarHrData.samples.lastOrNull()?.hr ?: 0
                    if (hr > 0) {
                        // Update state flow with the latest heart rate
                        _sensorData.value = _sensorData.value.copy(heartRate = hr)
                    }
                },
                { error ->
                    Log.e("POLAR", "Error streaming HR: ${error.message}", error)
                    hrDisposable?.dispose()
                    hrDisposable = null
                }
            )
    }

    // Subscribes to live accelerometer (movement) data from the Polar device
    private fun startAccStreaming(deviceId: String) {
        // Stop any existing accelerometer subscription
        accDisposable?.dispose()
        // Request settings first, then start ACC streaming
        accDisposable = api.requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ACC)
            .flatMapPublisher { settings ->
                api.startAccStreaming(deviceId, settings)
            }
            .subscribe(
                { polarAccData ->
                    // Get the latest accelerometer sample (X, Y, Z axes)
                    val lastSample = polarAccData.samples.lastOrNull()
                    if (lastSample != null) {
                        // Update state flow with latest movement data
                        _sensorData.value = _sensorData.value.copy(
                            accX = lastSample.x.toFloat(),
                            accY = lastSample.y.toFloat(),
                            accZ = lastSample.z.toFloat()
                        )
                    }
                },
                { error ->
                    Log.e("POLAR", "Error streaming ACC: ${error.message}", error)
                    accDisposable?.dispose()
                    accDisposable = null
                }
            )
    }

    // Connects to a Polar H10 device using its 8-character device ID
    fun connect(deviceId: String) {
        if (deviceId.isBlank()) return
        Log.d("POLAR", "connect() called for $deviceId")
        try {
            api.connectToDevice(deviceId)
            Log.d("POLAR", "connectToDevice() executed")
        } catch (e: Exception) {
            Log.e("POLAR", "Connection error: ${e.message}", e)
        }
    }

    // Disconnects from the current Polar H10 device
    fun disconnect(deviceId: String? = null) {
        cleanupStreams()
        try {
            val targetId = deviceId ?: _sensorData.value.deviceId
            if (targetId.isNotEmpty()) {
                api.disconnectFromDevice(targetId)
            }
        } catch (e: Exception) {
            Log.e("POLAR", "Disconnect error: ${e.message}", e)
        }
    }

    // Shuts down the Polar API and cleans up all connection resources
    fun shutDown(deviceId: String? = null) {
        disconnect(deviceId)
        try {
            api.cleanup()
            api.shutDown()
        } catch (e: Exception) {
            Log.e("POLAR", "Shutdown error: ${e.message}", e)
        }
    }

    // Stops and cleans up active RxJava data subscriptions (HR and ACC)
    private fun cleanupStreams() {
        hrDisposable?.dispose()
        hrDisposable = null
        accDisposable?.dispose()
        accDisposable = null
    }
}
