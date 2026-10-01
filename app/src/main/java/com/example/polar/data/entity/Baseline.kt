package com.example.polar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "baselines")
data class Baseline(
    @PrimaryKey
    val username: String,

    val baselineHr: Int,

    val createdAt: Long = System.currentTimeMillis()
)
