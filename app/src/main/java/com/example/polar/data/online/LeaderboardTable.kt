package com.example.polar.data.online

import android.util.Log
import io.github.jan.supabase.postgrest.from

/** Reads the online "leaderboard" view. */
//   getAll -> SELECT * FROM leaderboard
object LeaderboardTable {

    // Everyone who shares, highest points first. null if something went wrong (e.g. no internet).
    suspend fun getAll(): List<LeaderboardRow>? {
        return try {
            Supabase.client.from("leaderboard")
                .select()
                .decodeList<LeaderboardRow>()
                .sortedByDescending { it.totalPoints }
        } catch (e: Exception) {
            // Shows the reason in Logcat (search for "LeaderboardTable")
            Log.e("LeaderboardTable", "Online database error", e)
            null
        }
    }
}