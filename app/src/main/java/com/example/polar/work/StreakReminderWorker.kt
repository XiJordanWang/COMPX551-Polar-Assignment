package com.example.polar.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.polar.data.db.AppDatabase
import com.example.polar.data.prefs.SessionStore
import com.example.polar.data.prefs.SettingsStore
import com.example.polar.logic.CoachTrigger
import com.example.polar.logic.millisUntilNextReminder
import com.example.polar.logic.pickMessage
import com.example.polar.logic.shouldRemind
import com.example.polar.notify.CoachNotifier
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import kotlin.random.Random

// Runs in the background once a day (around 7pm), even if the app is closed.
// If the user has a streak but no workout today, the coach sends a STREAK_AT_RISK message.
class StreakReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext

        // A worker has no screen, so we get the logged-in user from SessionStore
        val user = SessionStore.getUser(context) ?: return Result.success()

        val workouts = AppDatabase.getDatabase(context).workoutDao().getWorkouts(user.username).first()
        if (shouldRemind(workouts, System.currentTimeMillis())) {
            val mode = SettingsStore.coachMode(context, user.username).first()
            // OFF mode gives null, so nothing is sent
            val message = pickMessage(mode, CoachTrigger.STREAK_AT_RISK, Random.Default, lastMessage = null)
            if (message != null) {
                CoachNotifier.showCoachMessage(context, message)
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "streak_reminder"

        // Called after login. KEEP = if it is already scheduled, don't schedule it again.
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StreakReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(millisUntilNextReminder(System.currentTimeMillis()), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        // Debug button: run the same check once, right now
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<StreakReminderWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
