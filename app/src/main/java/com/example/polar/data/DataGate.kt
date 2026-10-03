package com.example.polar.data

import android.content.Context
import com.example.polar.data.db.AppDatabase
import com.example.polar.data.entity.Baseline
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
    SAVED_LOCAL_ONLY
}

object DataGate {

    suspend fun saveWorkout(
        context: Context,
        username: String,
        workout: Workout,
        summary: WorkoutSummary
    ): SaveResult {
        // Always save to Room DB locally
        val db = AppDatabase.getDatabase(context)
        db.workoutDao().insert(workout)

        val mode = SettingsStore.privacyMode(context, username).first()
        return if (mode == PrivacyMode.SHARE) {
            val uploaded = WorkoutSummaryTable.insert(summary)
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
        AppDatabase.getDatabase(context).ecgDao().insert(check)
        return SaveResult.SAVED_LOCAL_ONLY
    }

    suspend fun saveAssessment(
        context: Context,
        username: String,
        assessment: Assessment
    ): SaveResult {
        val mode = SettingsStore.privacyMode(context, username).first()
        return if (mode == PrivacyMode.SHARE) {
            val saved = AssessmentTable.save(assessment)
            if (saved) SaveResult.SAVED_LOCAL_AND_ONLINE else SaveResult.SAVED_LOCAL_ONLY
        } else {
            SaveResult.SAVED_LOCAL_ONLY
        }
    }

    suspend fun saveBaseline(
        context: Context,
        username: String,
        baseline: Baseline
    ): SaveResult {
        AppDatabase.getDatabase(context).baselineDao().save(baseline)
        return SaveResult.SAVED_LOCAL_ONLY
    }
}
