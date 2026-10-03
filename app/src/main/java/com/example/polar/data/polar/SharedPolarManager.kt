package com.example.polar.data.polar

// A global singleton object that holds a single PolarManager instance.
// This ensures the entire app reuses one Bluetooth connection to the Polar H10.
object SharedPolarManager {

    // The shared PolarManager instance (null until created)
    var polarManager: PolarManager? = null
}
