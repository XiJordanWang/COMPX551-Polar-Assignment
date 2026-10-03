package com.example.polar.data.online

import android.util.Log
import io.github.jan.supabase.postgrest.from

/** Reads and writes the online "assessments" table. */
//   findByUsername -> SELECT * FROM assessments WHERE username = ?
//   save           -> INSERT ... ON CONFLICT (username) DO UPDATE  ("upsert")
//   deleteForUser  -> DELETE FROM assessments WHERE username = ?
object AssessmentTable {

    suspend fun findByUsername(username: String): Assessment? {
        return try {
            Supabase.client.from("assessments")
                .select { filter { eq("username", username) } }
                .decodeSingleOrNull<Assessment>()
        } catch (e: Exception) {
            // Shows the reason in Logcat (search for "AssessmentTable")
            Log.e("AssessmentTable", "Online database error", e)
            null
        }
    }

    // true = saved, false = something went wrong
    suspend fun save(assessment: Assessment): Boolean {
        return try {
            Supabase.client.from("assessments").upsert(assessment)
            true
        } catch (e: Exception) {
            // Shows the reason in Logcat (search for "AssessmentTable")
            Log.e("AssessmentTable", "Online database error", e)
            false
        }
    }

    // true = deleted, false = something went wrong
    suspend fun deleteForUser(username: String): Boolean {
        return try {
            Supabase.client.from("assessments").delete {
                filter { eq("username", username) }
            }
            true
        } catch (e: Exception) {
            // Shows the reason in Logcat (search for "AssessmentTable")
            Log.e("AssessmentTable", "Online database error", e)
            false
        }
    }
}