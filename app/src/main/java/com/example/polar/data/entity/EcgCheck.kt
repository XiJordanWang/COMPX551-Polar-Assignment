package com.example.polar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ecg_checks")
data class EcgCheck(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val time: Long,
    val restingHr: Int,
    val samples: String
)
