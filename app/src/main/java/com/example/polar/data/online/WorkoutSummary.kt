package com.example.polar.data.online

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One row in the online "workout_summaries" table. */
// Only the summary goes online, and only when the user has chosen "Uploaded to cloud".
// The heart rate of every second stays on the phone (Room).
@Serializable
data class WorkoutSummary(
    val username: String,
    val type: String,                                   // e.g. "Running"
    @SerialName("start_time") val startTime: Long,      // milliseconds
    @SerialName("duration_sec") val durationSec: Int,
    @SerialName("min_hr") val minHr: Int,
    @SerialName("avg_hr") val avgHr: Int,
    @SerialName("max_hr") val maxHr: Int,
    val points: Int
)