package com.example.polar.data.online

import io.github.jan.supabase.postgrest.from

// Writes to the online "workout_summaries" table.
//   insert -> INSERT INTO workout_summaries VALUES (...)
object WorkoutSummaryTable {

    // true = uploaded, false = something went wrong (the workout is still saved on the phone)
    suspend fun insert(summary: WorkoutSummary): Boolean {
        return try {
            Supabase.client.from("workout_summaries").insert(summary)
            true
        } catch (e: Exception) {
            false
        }
    }
}
