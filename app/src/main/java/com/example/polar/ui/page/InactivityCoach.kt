package com.example.polar.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.polar.BuildConfig
import com.example.polar.data.db.AppDatabase
import com.example.polar.data.polar.SensorData
import com.example.polar.data.prefs.SettingsStore
import com.example.polar.logic.CoachMode
import com.example.polar.logic.CoachTrigger
import com.example.polar.logic.PointsCalculator
import com.example.polar.logic.canSendAgain
import com.example.polar.logic.isInactive
import com.example.polar.logic.pickMessage
import com.example.polar.notify.CoachNotifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

// Runs during a workout (added to WorkoutPage with one line). Draws nothing.
// Every second it takes the current heart rate. If the last few minutes all stay
// within ±5 bpm of the resting baseline, the coach sends an INACTIVE message,
// then waits 5 minutes before it can send another one.
@Composable
fun InactivityCoach(username: String, sensorData: StateFlow<SensorData>) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    // Measured baseline (Chathurangi's baselines table), or 70 if never measured
    val baselineRow by remember { db.baselineDao().observeBaseline(username) }.collectAsState(initial = null)
    // Coach personality from Settings
    val coachMode by remember { SettingsStore.coachMode(context, username) }.collectAsState(initial = CoachMode.SUPPORTIVE)

    // 3 minutes normally, 30 seconds in debug builds so it is easy to test
    val windowSec = if (BuildConfig.DEBUG) 30 else 180

    LaunchedEffect(Unit) {
        // Our own list with exactly one value per second. The workout's heartRates list
        // can skip seconds, because a StateFlow doesn't send the same value twice.
        val samples = mutableListOf<Int>()
        var lastSentAt = 0L
        var lastMessage: String? = null

        while (true) {
            delay(1000)

            val data = sensorData.value
            // Only while connected: after a disconnect the last heart rate stays frozen,
            // and that would look like "resting"
            if (data.connected && data.heartRate > 0) {
                samples.add(data.heartRate)
            }
            // We only need the last window, so the list never grows too big
            while (samples.size > windowSec) {
                samples.removeAt(0)
            }

            val baseline = baselineRow?.baselineHr ?: PointsCalculator.DEFAULT_BASELINE_HR
            val now = System.currentTimeMillis()

            if (isInactive(samples, baseline, windowSec) && canSendAgain(lastSentAt, now)) {
                // OFF mode gives null, so nothing is sent
                val message = pickMessage(coachMode, CoachTrigger.INACTIVE, Random.Default, lastMessage)
                if (message != null) {
                    CoachNotifier.showCoachMessage(context, message)
                    lastMessage = message
                }
                // Start the cool-down even in OFF mode, so we don't check this every second
                lastSentAt = now
            }
        }
    }
}
