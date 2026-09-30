package com.example.polar.data.online

import io.github.jan.supabase.postgrest.from

// Reads and writes the online "assessments" table.
//   findByUsername -> SELECT * FROM assessments WHERE username = ?
//   save           -> INSERT ... ON CONFLICT (username) DO UPDATE  ("upsert")
object AssessmentTable {

    suspend fun findByUsername(username: String): Assessment? {
        return try {
            Supabase.client.from("assessments")
                .select { filter { eq("username", username) } }
                .decodeSingleOrNull<Assessment>()
        } catch (e: Exception) {
            null
        }
    }

    // true = saved, false = something went wrong
    suspend fun save(assessment: Assessment): Boolean {
        return try {
            Supabase.client.from("assessments").upsert(assessment)
            true
        } catch (e: Exception) {
            false
        }
    }
}
