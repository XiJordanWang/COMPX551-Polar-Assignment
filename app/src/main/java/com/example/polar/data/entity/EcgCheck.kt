package com.example.polar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One saved 30-second ECG check, as a row in the "ecg_checks" table. */
@Entity(tableName = "ecg_checks")
data class EcgCheck(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val time: Long,        // milliseconds, from System.currentTimeMillis()
    val restingHr: Int,
    val samples: String    // every ECG value from the check, e.g. "100,120,110"
)