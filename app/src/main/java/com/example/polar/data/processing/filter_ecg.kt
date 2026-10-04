package com.example.polar.data.processing

import kotlin.math.PI

class EcgFilter(private val samplingRate: Double = 130.0) {
    // Frequencies we want to keep:
    // 0.5 Hz to 40 Hz
    private val lowCutoff = 0.5
    private val highCutoff = 40.0

    // Previous values needed for real-time filtering
    private var previousInput = 0.0
    private var previousHighPassOutput = 0.0
    private var previousLowPassOutput = 0.0


    // Removes slow baseline drift below ~0.5 Hz
    private fun highPassFilter(input: Double): Double {

        val dt = 1.0 / samplingRate
        val rc = 1.0 / (2.0 * PI * lowCutoff)

        val alpha = rc / (rc + dt)

        val output = alpha * (
            previousHighPassOutput +
            input -
            previousInput
        )

        previousInput = input
        previousHighPassOutput = output

        return output
    }


    // Reduces high-frequency noise above ~40 Hz
    private fun lowPassFilter(input: Double): Double {

        val dt = 1.0 / samplingRate
        val rc = 1.0 / (2.0 * PI * highCutoff)

        val alpha = dt / (rc + dt)

        val output = previousLowPassOutput +
                alpha * (input - previousLowPassOutput)

        previousLowPassOutput = output

        return output
    }


    // Run each ECG sample through both filters
    fun filter(input: Double): Double {

        val highPassed = highPassFilter(input)

        return lowPassFilter(highPassed)
    }


    // Reset filter when starting a new ECG recording
    fun reset() {
        previousInput = 0.0
        previousHighPassOutput = 0.0
        previousLowPassOutput = 0.0
    }
}