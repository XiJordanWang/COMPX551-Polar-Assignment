package com.example.polar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.polar.data.entity.Workout
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert
    suspend fun insert(workout: Workout)

    // Flow = the list updates by itself when a new workout is saved
    @Query("SELECT * FROM workouts WHERE username = :username ORDER BY startTime DESC")
    fun getWorkouts(username: String): Flow<List<Workout>>

    // Start times of this user's workouts, used so demo data isn't added twice
    @Query("SELECT startTime FROM workouts WHERE username = :username")
    suspend fun getStartTimes(username: String): List<Long>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun findById(id: Long): Workout?

    @Query("DELETE FROM workouts WHERE username = :username")
    suspend fun deleteForUser(username: String)
}
