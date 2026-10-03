package com.example.polar.data.online

import android.util.Log
import io.github.jan.supabase.postgrest.from

/** Writes to the online "workout_summaries" table. */
//   insert        -> INSERT INTO workout_summaries VALUES (...)
//   deleteForUser -> DELETE FROM workout_summaries WHERE username = ?
object WorkoutSummaryTable {

    // true = uploaded, false = something went wrong (the workout is still saved on the phone)
    suspend fun insert(summary: WorkoutSummary): Boolean {
        return try {
            Supabase.client.from("workout_summaries").insert(summary)
            true
        } catch (e: Exception) {
            // Shows the reason in Logcat (search for "WorkoutSummaryTable")
            Log.e("WorkoutSummaryTable", "Online database error", e)
            false
        }
    }

    // Removes all of this user's summaries (when they switch to "Saved locally" or delete their data).
    // true = deleted, false = something went wrong
    suspend fun deleteForUser(username: String): Boolean {
        return try {
            Supabase.client.from("workout_summaries").delete {
                filter { eq("username", username) }
            }
            true
        } catch (e: Exception) {
            // Shows the reason in Logcat (search for "WorkoutSummaryTable")
            Log.e("WorkoutSummaryTable", "Online database error", e)
            false
        }
    }
}