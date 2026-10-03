package com.example.polar.data.model

/** The list of sports the user can choose from, each with its emoji. */
data class WorkoutType(val name: String, val emoji: String)

// All the workouts the user can pick from before starting
val workoutTypes = listOf(
    WorkoutType("Running", "🏃"),
    WorkoutType("Walking", "🚶"),
    WorkoutType("Swimming", "🏊"),
    WorkoutType("Hiking", "🥾"),
    WorkoutType("Badminton", "🏸"),
    WorkoutType("Rugby", "🏉"),
    WorkoutType("Tennis", "🎾"),
    WorkoutType("Strength Training", "🏋️")
)

// "Running" -> "🏃"
fun emojiFor(type: String): String {
    return workoutTypes.find { it.name == type }?.emoji ?: "🏃"
}