package com.example.polar.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.polar.data.entity.Assessment
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentDao {

    // Insert, or update if this user already has one
    @Upsert
    suspend fun save(assessment: Assessment)

    // test again
    @Query("SELECT * FROM assessments WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): Assessment?

    // Same as above but updates by itself when the assessment changes
    @Query("SELECT * FROM assessments WHERE username = :username LIMIT 1")
    fun observe(username: String): Flow<Assessment?>
}
