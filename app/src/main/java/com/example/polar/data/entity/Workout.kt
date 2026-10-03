package com.example.polar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One finished workout, as a row in the "workouts" table. */
@Entity(tableName = "workouts")
data class Workout(@PrimaryKey(autoGenerate = true) val id: Long = 0,
                   val username: String,
                   val type: String,        // e.g. "Running"
                   val startTime: Long,     // milliseconds, from System.currentTimeMillis()
                   val durationSec: Int,
                   val minHr: Int,
                   val avgHr: Int,
                   val maxHr: Int,
                   val heartRates: String)  // one bpm per second, e.g. "80,82,85"

// "80,82,85" -> [80, 82, 85]
fun Workout.heartRateList(): List<Int> {
    if (heartRates.isEmpty()) return emptyList()
    return heartRates.split(",").map { it.toInt() }
}