package com.example.polar.data.online

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// One row in the online "assessments" table. One assessment per user,
// so username is the primary key. Saving again replaces the old one.
@Serializable
data class Assessment(
    val username: String,
    val gender: String,                                          // "Male" or "Female"
    val age: Int,
    @SerialName("height_cm") val heightCm: Int,
    @SerialName("weight_kg") val weightKg: Double,
    @SerialName("workouts_per_week") val workoutsPerWeek: String, // "0", "1-2", "3-4" or "5+"
    val intensity: String                                        // "Light", "Moderate" or "Hard"
)
