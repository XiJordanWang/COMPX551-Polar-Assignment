package com.example.polar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.polar.data.entity.EcgCheck
import kotlinx.coroutines.flow.Flow

@Dao
interface EcgDao {

    @Insert
    suspend fun insert(ecgCheck: EcgCheck)

    @Query("SELECT * FROM ecg_checks WHERE username = :username ORDER BY time DESC")
    fun getChecks(username: String): Flow<List<EcgCheck>>
}
