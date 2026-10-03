package com.example.polar.data.online

import android.util.Log
import io.github.jan.supabase.postgrest.from

/** Reads and writes the online "users" table. */
// Like SQL:
//   findByUsername -> SELECT * FROM users WHERE username = ?
//   insert         -> INSERT INTO users VALUES (...)
//   setSharing     -> UPDATE users SET sharing = ? WHERE username = ?
//   setStreak      -> UPDATE users SET streak = ? WHERE username = ?
//   deleteUser     -> DELETE FROM users WHERE username = ?
// Network can fail (no internet), so we catch the error and return null / false
// instead of crashing the app.
object UserTable {

    suspend fun findByUsername(username: String): User? {
        return try {
            Supabase.client.from("users")
                .select { filter { eq("username", username) } }
                .decodeSingleOrNull<User>()
        } catch (e: Exception) {
            // Shows the reason in Logcat (search for "UserTable")
            Log.e("UserTable", "Online database error", e)
            null
        }
    }

    // true = saved, false = something went wrong
    suspend fun insert(user: User): Boolean {
        return try {
            Supabase.client.from("users").insert(user)
            true
        } catch (e: Exception) {
            // Shows the reason in Logcat (search for "UserTable")
            Log.e("UserTable", "Online database error", e)
            false
        }
    }

    // Turns leaderboard sharing on or off. true = updated, false = something went wrong
    suspend fun setSharing(username: String, sharing: Boolean): Boolean {
        return try {
            Supabase.client.from("users").update({
                set("sharing", sharing)
            }) {
                filter { eq("username", username) }
            }
            true
        } catch (e: Exception) {
            // Shows the reason in Logcat (search for "UserTable")
            Log.e("UserTable", "Online database error", e)
            false
        }
    }

    // Updates the streak shown on the leaderboard. true = updated, false = something went wrong
    suspend fun setStreak(username: String, streak: Int): Boolean {
        return try {
            Supabase.client.from("users").update({
                set("streak", streak)
            }) {
                filter { eq("username", username) }
            }
            true
        } catch (e: Exception) {
            // Shows the reason in Logcat (search for "UserTable")
            Log.e("UserTable", "Online database error", e)
            false
        }
    }

    // Deletes the account. The database also removes their assessment and workout summaries (on delete cascade).
    // true = deleted, false = something went wrong
    suspend fun deleteUser(username: String): Boolean {
        return try {
            Supabase.client.from("users").delete {
                filter { eq("username", username) }
            }
            true
        } catch (e: Exception) {
            // Shows the reason in Logcat (search for "UserTable")
            Log.e("UserTable", "Online database error", e)
            false
        }
    }
}