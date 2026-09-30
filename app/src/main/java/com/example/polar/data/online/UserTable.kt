package com.example.polar.data.online

import io.github.jan.supabase.postgrest.from

// Reads and writes the online "users" table.
// Like SQL:
//   findByUsername -> SELECT * FROM users WHERE username = ?
//   insert         -> INSERT INTO users VALUES (...)
// Network can fail (no internet), so we catch the error and return null / false
// instead of crashing the app.
object UserTable {

    suspend fun findByUsername(username: String): User? {
        return try {
            Supabase.client.from("users")
                .select { filter { eq("username", username) } }
                .decodeSingleOrNull<User>()
        } catch (e: Exception) {
            null
        }
    }

    // true = saved, false = something went wrong
    suspend fun insert(user: User): Boolean {
        return try {
            Supabase.client.from("users").insert(user)
            true
        } catch (e: Exception) {
            false
        }
    }
}
