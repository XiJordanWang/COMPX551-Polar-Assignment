package com.example.polar.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.polar.data.entity.Baseline
import kotlinx.coroutines.flow.Flow

/** Saves and reads each user's resting heart rate baseline (one per user). */
@Dao
interface BaselineDao {

    @Upsert
    suspend fun save(baseline: Baseline)

    @Query(
        "SELECT * FROM baselines WHERE username = :username"
    )
    fun observeBaseline(
        username: String
    ): Flow<Baseline?>

    @Query("DELETE FROM baselines WHERE username = :username")
    suspend fun deleteForUser(username: String)
}