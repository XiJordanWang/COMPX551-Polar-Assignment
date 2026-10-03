package com.example.polar.data.online

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One row of the online "leaderboard" view (see supabase/schema.sql). */
// A view is a saved SELECT: it joins users with workout_summaries, adds up the points,
// and only includes users who have chosen "Uploaded to cloud".
@Serializable
data class LeaderboardRow(
    val username: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("total_points") val totalPoints: Int,
    val workouts: Int,
    val streak: Int = 0
)