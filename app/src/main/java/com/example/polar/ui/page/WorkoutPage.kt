package com.example.polar.ui.page

import android.app.Activity
import android.os.Bundle
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.polar.data.db.AppDatabase
import com.example.polar.data.entity.Workout
import com.example.polar.data.online.AssessmentTable
import com.example.polar.data.online.WorkoutSummary
import com.example.polar.data.online.WorkoutSummaryTable
import com.example.polar.logic.formatTime
import com.example.polar.logic.maxHeartRate
import com.example.polar.logic.workoutPoints
import com.example.polar.ui.theme.Orange
import com.example.polar.ui.theme.PolarTheme
import com.example.polar.ui.theme.WorkSans
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class WorkoutPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Picked on the main page, e.g. "Running"
        val workoutType = intent.getStringExtra("workoutType") ?: "Workout"
        val username = intent.getStringExtra("username") ?: ""

        setContent {
            PolarTheme {
                WorkoutScreen(workoutType, username)
            }
        }
    }
}

@Composable
fun WorkoutScreen(workoutType: String, username: String) {
    val context = LocalContext.current
    // 200 gives the default zones (100 / 120 / 140 / 160).
    // If the user did the assessment, use 220 - age instead.
    var userMaxHr by remember { mutableIntStateOf(200) }
    LaunchedEffect(Unit) {
        // From the online assessments table. If there is no internet we keep 200.
        val assessment = AssessmentTable.findByUsername(username)
        if (assessment != null) {
            userMaxHr = maxHeartRate(assessment.age)
        }
    }
    // One heart rate per second
    val heartRates = remember { mutableStateListOf<Int>() }
    val startTime = remember { System.currentTimeMillis() }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    // TODO: replace this fake data with the real heart rate from the Polar SDK
    LaunchedEffect(Unit) {
        var hr = 75
        while (true) {
            delay(1000)
            hr = (hr + Random.nextInt(-3, 6)).coerceIn(60, 185)
            heartRates.add(hr)
        }
    }

    val seconds = heartRates.size
    val currentHr = heartRates.lastOrNull() ?: 0
    val minHr = heartRates.minOrNull() ?: 0
    val maxHr = heartRates.maxOrNull() ?: 0
    val avgHr = if (heartRates.isEmpty()) 0 else heartRates.average().toInt()
    // Only show the last 60 seconds on the line chart
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
            Text(
                text = workoutType,
                color = Color.White,
                fontSize = 36.sp,
                fontFamily = WorkSans,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(label = "Time", value = formatTime(seconds), unit = "", modifier = Modifier.weight(1f))
                StatCard(label = "Heart Rate", value = "$currentHr", unit = "bpm", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Min / Avg / Max for the whole workout
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row {
                    SmallStat(label = "Min", value = minHr, modifier = Modifier.weight(1f))
                    SmallStat(label = "Avg", value = avgHr, modifier = Modifier.weight(1f))
                    SmallStat(label = "Max", value = maxHr, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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

                    // Swipe left / right to change chart
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

                    // Page dots
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

            Button(
                onClick = {
                    if (heartRates.isEmpty()) {
                        // Nothing recorded, don't save an empty workout
                        (context as Activity).finish()
                    } else {
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
                        // Only the summary goes online, the heart rate of every second stays on the phone
                        val summary = WorkoutSummary(
                            username = username,
                            type = workoutType,
                            startTime = startTime,
                            durationSec = heartRates.size,
                            minHr = minHr,
                            avgHr = avgHr,
                            maxHr = maxHr,
                            points = workoutPoints(heartRates, userMaxHr)
                        )
                        scope.launch {
                            // 1. Save the full workout on the phone (Room)
                            AppDatabase.getDatabase(context).workoutDao().insert(workout)
                            // 2. Upload the summary (Supabase). If it fails, the workout is still saved locally.
                            val uploaded = WorkoutSummaryTable.insert(summary)
                            val message = if (uploaded) "Workout saved" else "Workout saved on this phone (upload failed)"
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

@Composable
fun SmallStat(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
        Text(text = "$value", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(text = "bpm", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}
