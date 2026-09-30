package com.example.polar.logic

import com.example.polar.data.online.Assessment
import kotlin.math.roundToInt

// BMI = weight (kg) / height (m)^2
fun calculateBmi(heightCm: Int, weightKg: Double): Double {
    val heightM = heightCm / 100.0
    return weightKg / (heightM * heightM)
}

fun bmiCategory(bmi: Double): String {
    return when {
        bmi < 18.5 -> "Underweight"
        bmi < 25 -> "Normal"
        bmi < 30 -> "Overweight"
        else -> "Obese"
    }
}

// Common simple formula
fun maxHeartRate(age: Int): Int {
    return 220 - age
}

// Calories burned, using the Keytel formula (2005).
// It needs gender, age and weight from the assessment, and the average heart rate.
// The formula gives kJ per minute, we divide by 4.184 to get kcal.
fun caloriesBurned(assessment: Assessment, avgHr: Int, durationSec: Int): Int {
    val kjPerMinute = if (assessment.gender == "Male") {
        -55.0969 + 0.6309 * avgHr + 0.1988 * assessment.weightKg + 0.2017 * assessment.age
    } else {
        -20.4022 + 0.4472 * avgHr - 0.1263 * assessment.weightKg + 0.074 * assessment.age
    }
    val minutes = durationSec / 60.0
    val kcal = kjPerMinute * minutes / 4.184
    // Very low heart rates can give a negative number
    return if (kcal < 0) 0 else kcal.roundToInt()
}

// Where each zone starts: 50%, 60%, 70% and 80% of max heart rate.
// Below the first one is "Rest", above the last one is "Maximum".
fun zoneLimits(maxHr: Int): List<Int> {
    return listOf(0.5, 0.6, 0.7, 0.8).map { (maxHr * it).roundToInt() }
}
