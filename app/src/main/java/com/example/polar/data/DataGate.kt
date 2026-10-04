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
import com.example.polar.logic.PointsCalculator
import com.example.polar.logic.calculateWorkoutPoints
import com.example.polar.logic.streakDays
import kotlinx.coroutines.flow.first

/** Every save in the app goes through here, so the user's privacy mode is checked in one place. */
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

        // Only upload the summary (and update the streak) in "Uploaded to cloud" mode
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

    // Re-uploads every workout on the phone (used when the user turns on "Uploaded to cloud").
    // Uses calculateWorkoutPoints, the same as the home screen, so the daily goal bonus is included
    // and the online total matches the plant. Returns how many workouts were uploaded.
    suspend fun uploadAllWorkouts(context: Context, username: String): Int {
        val db = AppDatabase.getDatabase(context)
        val localWorkouts = db.workoutDao().getWorkouts(username).first()
        val baseline = db.baselineDao().observeBaseline(username).first()?.baselineHr
            ?: PointsCalculator.DEFAULT_BASELINE_HR
        val age = loadAssessment(context, username)?.age ?: 25

        // Clear old online summaries first so nothing is counted twice
        WorkoutSummaryTable.deleteForUser(username)

        val summaries = localWorkouts.map { w ->
            WorkoutSummary(
                username = username,
                type = w.type,
                startTime = w.startTime,
                durationSec = w.durationSec,
                minHr = w.minHr,
                avgHr = w.avgHr,
                maxHr = w.maxHr,
                points = calculateWorkoutPoints(w, localWorkouts, baseline, age)
            )
        }
        val uploaded = if (WorkoutSummaryTable.insertAll(summaries)) summaries.size else 0

        UserTable.setStreak(username, streakDays(localWorkouts))
        return uploaded
    }

    // Load assessment based on privacy mode (local DataStore or online table)
    suspend fun loadAssessment(context: Context, username: String): Assessment? {
        val mode = SettingsStore.privacyMode(context, username).first()
        return if (mode == PrivacyMode.FULL) {
            SettingsStore.getAssessment(context, username) ?: AssessmentTable.findByUsername(username)
        } else {
            AssessmentTable.findByUsername(username) ?: SettingsStore.getAssessment(context, username)
        }
    }

    // ECG checks are only ever saved on the phone
    suspend fun saveEcg(
        context: Context,
        username: String,
        check: EcgCheck
    ): SaveResult {
        AppDatabase.getDatabase(context).ecgDao().insert(check)
        return SaveResult.SAVED_LOCAL_ONLY
    }

    // Saved locally in SettingsStore (JSON), and uploaded to Supabase in "Uploaded to cloud" mode.
    suspend fun saveAssessment(
        context: Context,
        username: String,
        assessment: Assessment
    ): SaveResult {
        val mode = SettingsStore.privacyMode(context, username).first()
        SettingsStore.setAssessment(context, username, assessment)
        return if (mode == PrivacyMode.SHARE) {
            val saved = AssessmentTable.save(assessment)
            if (saved) SaveResult.SAVED_LOCAL_AND_ONLINE else SaveResult.SAVED_LOCAL_ONLY
        } else {
            SaveResult.SAVED_LOCAL_ONLY
        }
    }

    // Baselines are only ever saved on the phone
    suspend fun saveBaseline(
        context: Context,
        username: String,
        baseline: Baseline
    ): SaveResult {
        AppDatabase.getDatabase(context).baselineDao().save(baseline)
        return SaveResult.SAVED_LOCAL_ONLY
    }
}