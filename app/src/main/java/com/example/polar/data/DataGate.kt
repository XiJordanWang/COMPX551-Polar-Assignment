package com.example.polar.data

import android.content.Context
import com.example.polar.data.db.AppDatabase
import com.example.polar.data.entity.EcgCheck
import com.example.polar.data.entity.Workout
import com.example.polar.data.online.Assessment
import com.example.polar.data.online.AssessmentTable
import com.example.polar.data.online.UserTable
import com.example.polar.data.online.WorkoutSummary
import com.example.polar.data.online.WorkoutSummaryTable
import com.example.polar.data.prefs.PrivacyMode
import com.example.polar.data.prefs.SettingsStore
import com.example.polar.logic.streakDays
import kotlinx.coroutines.flow.first

enum class SaveResult {
    SAVED_LOCAL_AND_ONLINE,
    SAVED_LOCAL_ONLY,
    READ_ONLY
}

object DataGate {

    suspend fun saveWorkout(
        context: Context,
        username: String,
        workout: Workout,
        summary: WorkoutSummary
    ): SaveResult {
        val mode = SettingsStore.privacyMode(context, username).first()
        if (mode == PrivacyMode.READ_ONLY) {
            return SaveResult.READ_ONLY
        }

        // Save to Room
        val db = AppDatabase.getDatabase(context)
        db.workoutDao().insert(workout)

        // Upload only if SHARE or FULL
        return if (mode == PrivacyMode.SHARE || mode == PrivacyMode.FULL) {
            val uploaded = WorkoutSummaryTable.insert(summary)
            // Also update streak online
            val allWorkouts = db.workoutDao().getWorkouts(username).first()
            val streak = streakDays(allWorkouts)
            UserTable.setStreak(username, streak)
            UserTable.setSharing(username, true)
            if (uploaded) SaveResult.SAVED_LOCAL_AND_ONLINE else SaveResult.SAVED_LOCAL_ONLY
        } else {
            SaveResult.SAVED_LOCAL_ONLY
        }
    }

    suspend fun saveEcg(
        context: Context,
        username: String,
        check: EcgCheck
    ): SaveResult {
        val mode = SettingsStore.privacyMode(context, username).first()
        if (mode == PrivacyMode.READ_ONLY) {
            return SaveResult.READ_ONLY
        }
        AppDatabase.getDatabase(context).ecgDao().insert(check)
        return SaveResult.SAVED_LOCAL_ONLY
    }

    suspend fun saveAssessment(
        context: Context,
        username: String,
        assessment: Assessment
    ): SaveResult {
        val mode = SettingsStore.privacyMode(context, username).first()
        if (mode == PrivacyMode.READ_ONLY) {
            return SaveResult.READ_ONLY
        }
        val saved = AssessmentTable.save(assessment)
        return if (saved) SaveResult.SAVED_LOCAL_AND_ONLINE else SaveResult.SAVED_LOCAL_ONLY
    }
}
