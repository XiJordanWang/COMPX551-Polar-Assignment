package com.example.polar.ui.page

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.polar.data.DataGate
import com.example.polar.data.SaveResult
import com.example.polar.data.db.AppDatabase
import com.example.polar.data.entity.Workout
import com.example.polar.data.online.WorkoutSummary
import com.example.polar.logic.ExerciseZone
import com.example.polar.logic.formatTime
import com.example.polar.logic.maxHeartRate
import com.example.polar.logic.PointsCalculator
import com.example.polar.service.WorkoutService
import com.example.polar.ui.theme.Orange
import com.example.polar.ui.theme.PolarTheme
import com.example.polar.ui.theme.WorkSans
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.polar.data.polar.SharedPolarManager

// Activity for tracking live workouts, streaming real-time heart rate, and recording exercise sessions.
class WorkoutPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Extract workout type (e.g. Running) and username passed from MainPage
        val workoutType = intent.getStringExtra("workoutType") ?: "Workout"
        val username = intent.getStringExtra("username") ?: ""

        setContent {
            PolarTheme {
                WorkoutScreen(workoutType, username)
            }
        }
    }
}

// Main workout tracking composable screen
@Composable
fun WorkoutScreen(workoutType: String, username: String) {
    val context = LocalContext.current

    // Retrieve shared PolarManager instance for live BLE streaming
    val polarManager =
        SharedPolarManager.polarManager
            ?: return

    // Observe live sensor data (heart rate & accelerometer) from PolarManager
    val sensorData by polarManager.sensorData.collectAsState()

    // Manage foreground service lifecycle to keep heart rate recording active in background
    DisposableEffect(Unit) {
        WorkoutService.start(context)
        onDispose { WorkoutService.stop(context) }
    }

    // Inactivity coach: monitors heart rate stability and alerts if resting during exercise
    InactivityCoach(username = username, sensorData = polarManager.sensorData)

    val db = remember { AppDatabase.getDatabase(context) }
    val baselineEntity by db.baselineDao().observeBaseline(username).collectAsState(initial = null)
    val baseline = baselineEntity?.baselineHr ?: PointsCalculator.DEFAULT_BASELINE_HR
    var age by remember { mutableIntStateOf(25) }

    // Calculate maximum heart rate based on user's assessment age, defaulting to 200 BPM
    var userMaxHr by remember { mutableIntStateOf(200) }
    LaunchedEffect(Unit) {
        val assessment = DataGate.loadAssessment(context, username)
        if (assessment != null) {
            userMaxHr = maxHeartRate(assessment.age)
            age = assessment.age
        }
    }

    // List storing one heart rate sample per second for the duration of the workout
    val heartRates = remember { mutableStateListOf<Int>() }

    val livePoints = remember(heartRates, baseline, age) {
        PointsCalculator.calculate(heartRates, baseline, age)
    }
    val currentZone = remember(sensorData.heartRate, baseline, age) {
        if (sensorData.heartRate <= 0) "Resting"
        else {
            when (PointsCalculator.getZone(sensorData.heartRate, baseline, age)) {
                ExerciseZone.BELOW_TARGET -> "Below Target"
                ExerciseZone.LOW -> "Low (+10)"
                ExerciseZone.MODERATE -> "Moderate (+20)"
                ExerciseZone.INTENSE -> "Intense (+30)"
                ExerciseZone.ABOVE_CAP -> "Above Cap"
            }
        }
    }

    // Coroutine loop sampling heart rate values once per second from PolarManager
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            val hr = polarManager.sensorData.value.heartRate
            if (hr > 0) {
                heartRates.add(hr)
                Log.d(
                    "POLAR_HR",
                    "WorkoutPage recorded HR = $hr (total samples: ${heartRates.size})"
                )
            }
        }
    }

    val startTime = remember { System.currentTimeMillis() }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    // Compute live workout summary statistics
    val seconds = heartRates.size
    val currentHr = if (sensorData.heartRate > 0) sensorData.heartRate else (heartRates.lastOrNull() ?: 0)
    val minHr = heartRates.minOrNull() ?: 0
    val maxHr = heartRates.maxOrNull() ?: 0
    val avgHr = if (heartRates.isEmpty()) 0 else heartRates.average().toInt()

    // Slice recent 60-second window for real-time line chart rendering
    val lastMinute = heartRates.takeLast(60)
    val firstSecond = seconds - lastMinute.size + 1

    Box(modifier = Modifier.fillMaxSize()) {
        GardenBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
        ) {

            // Workout title header
            Text(
                text = workoutType,
                color = Color.White,
                fontSize = 36.sp,
                fontFamily = WorkSans,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Connection notification banner shown when Polar H10 is disconnected
            if (!sensorData.connected) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚠️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Polar H10 Disconnected",
                                color = Color(0xFFFFD54F),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Primary metrics: elapsed time and current live heart rate
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(label = "Time", value = formatTime(seconds), unit = "", modifier = Modifier.weight(1f))
                StatCard(label = "Heart Rate", value = "$currentHr", unit = "bpm", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live points and current zone
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(label = "Live Points", value = "$livePoints", unit = "pts", modifier = Modifier.weight(1f))
                StatCard(label = "Zone", value = currentZone, unit = "", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary metrics: minimum, average, and maximum heart rate
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row {
                    SmallStat(label = "Min", value = minHr, modifier = Modifier.weight(1f))
                    SmallStat(label = "Avg", value = avgHr, modifier = Modifier.weight(1f))
                    SmallStat(label = "Max", value = maxHr, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chart area: swipeable view switching between 60s heart rate trend and intensity gauge
            GlassCard(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) {
                Column {
                    Text(
                        text = if (pagerState.currentPage == 0) "Heart Rate (last 60s)" else "Intensity",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { page ->
                        if (page == 0) {
                            EChartsView(
                                fileName = "line_chart.html",
                                script = "setData($lastMinute, $firstSecond)"
                            )
                        } else {
                            EChartsView(
                                fileName = "gauge.html",
                                script = "setMaxHr($userMaxHr); setValue($currentHr)"
                            )
                        }
                    }

                    // Pager indicator dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (i in 0 until 2) {
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(8.dp)
                                    .background(
                                        if (pagerState.currentPage == i) Orange else Color.White.copy(alpha = 0.4f),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stop workout button: computes final points and saves workout data locally and online
            Button(
                onClick = {
                    if (heartRates.isEmpty()) {
                        (context as Activity).finish()
                    } else {
                        val points = PointsCalculator.calculate(heartRates, baseline, age)
                        val workout = Workout(
                            username = username,
                            type = workoutType,
                            startTime = startTime,
                            durationSec = heartRates.size,
                            minHr = minHr,
                            avgHr = avgHr,
                            maxHr = maxHr,
                            heartRates = heartRates.joinToString(",")
                        )
                        val summary = WorkoutSummary(
                            username = username,
                            type = workoutType,
                            startTime = startTime,
                            durationSec = heartRates.size,
                            minHr = minHr,
                            avgHr = avgHr,
                            maxHr = maxHr,
                            points = points
                        )
                        scope.launch {
                            val result = DataGate.saveWorkout(context, username, workout, summary)
                            val message = when (result) {
                                SaveResult.SAVED_LOCAL_AND_ONLINE -> "Workout saved & uploaded to cloud"
                                SaveResult.SAVED_LOCAL_ONLY -> "Workout saved on this phone"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            (context as Activity).finish()
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111111), contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = "Stop Workout", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// Compact statistic item component (Min, Avg, Max)
@Composable
fun SmallStat(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
        Text(text = "$value", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(text = "bpm", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}
