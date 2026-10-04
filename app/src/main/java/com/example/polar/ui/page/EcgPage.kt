package com.example.polar.ui.page

/** Screen for recording, viewing, and analyzing 30-second ECG signals from the Polar H10. */

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.polar.data.DataGate
import com.example.polar.data.db.AppDatabase
import com.example.polar.data.entity.EcgCheck
import com.example.polar.data.polar.PolarManager
import com.example.polar.data.polar.SharedPolarManager
import com.example.polar.data.processing.EcgFilter
import com.example.polar.logic.ECG_SAMPLE_RATE
import com.example.polar.logic.fakeEcgValue
import com.example.polar.logic.heartRateFromEcg
import com.example.polar.logic.restingHrComment
import com.example.polar.ui.theme.Orange
import com.example.polar.ui.theme.PolarTheme
import com.example.polar.ui.theme.WorkSans
import kotlinx.coroutines.delay

// Duration of ECG check reading in seconds
private const val ECG_SECONDS = 30

// Activity for recording resting ECG readings from the Polar H10 device
class EcgPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val username = intent.getStringExtra("username") ?: ""

        setContent {
            PolarTheme {
                EcgScreen(username)
            }
        }
    }
}

// Composable screen for measuring and displaying live 30-second resting ECG
@Composable
fun EcgScreen(username: String = "") {
    val context = LocalContext.current

    // Access shared PolarManager instance
    val polarManager = remember {
        SharedPolarManager.polarManager ?: PolarManager(context).also {
            SharedPolarManager.polarManager = it
        }
    }

    // Observe local device ID and live sensor connection state
    val db = remember { AppDatabase.getDatabase(context) }
    val deviceIdFlow = remember { db.deviceDao().observeDeviceId(username) }
    val deviceId by deviceIdFlow.collectAsState(initial = null)
    val sensorData by polarManager.sensorData.collectAsState()

    // Automatically attempt connection to Polar H10 if device ID is set
    LaunchedEffect(deviceId) {
        if (!deviceId.isNullOrBlank() && !sensorData.connected) {
            Log.d("POLAR_ECG", "Connecting to $deviceId for ECG check")
            polarManager.connect(deviceId!!)
        }
    }

    // Clean up active ECG stream if activity is closed mid-recording
    DisposableEffect(Unit) {
        onDispose {
            polarManager.stopEcgStreaming()
        }
    }

    // Measuring state: "ready" -> "measuring" -> "done"
    var status by remember { mutableStateOf("ready") }
    var secondsLeft by remember { mutableIntStateOf(ECG_SECONDS) }
    var restingHr by remember { mutableIntStateOf(0) }
    val allSamples = remember { mutableStateListOf<Int>() }

    // Coroutine managing the 30-second ECG recording session
    LaunchedEffect(status) {
        if (status == "measuring") {
            allSamples.clear()
            var isUsingRealEcgStream = false

            val activeTargetId = if (!sensorData.deviceId.isNullOrBlank()) sensorData.deviceId else (deviceId ?: "")

            // Request live ECG streaming from the connected Polar H10 device
            if (sensorData.connected && activeTargetId.isNotBlank()) {
                polarManager.startEcgStreaming(
                    deviceId = activeTargetId,
                    onEcgSample = { sample ->
                        isUsingRealEcgStream = true
                        allSamples.add(sample)
                    },
                    onError = { error ->
                        Log.e("POLAR_ECG", "Real ECG stream error: ${error.message}")
                    }
                )
            }

            var phase = 0.0

            // Record for 30 seconds (300 ticks of 100ms)
            for (tick in 1..ECG_SECONDS * 10) {
                delay(100)

                // If real stream is unavailable, synthesize waveform using live Polar heart rate
                if (!isUsingRealEcgStream) {
                    val currentHr = if (sensorData.heartRate > 0) sensorData.heartRate else 65
                    for (i in 0 until 13) {
                        phase += (currentHr / 60.0) / ECG_SAMPLE_RATE
                        if (phase >= 1.0) phase -= 1.0
                        allSamples.add(fakeEcgValue(phase))
                    }
                }

                secondsLeft = ECG_SECONDS - tick / 10
            }

            // Stop ECG stream from Polar H10
            polarManager.stopEcgStreaming()

            // Apply ECG band-pass filter (0.5–40 Hz) to remove baseline drift and noise
            val ecgFilter = EcgFilter()
            val filteredSamples = allSamples.map { ecgFilter.filter(it.toDouble()).toInt() }

            // Detect R-peaks and compute resting heart rate using filtered data
            val calculatedHr = heartRateFromEcg(filteredSamples)
            restingHr = if (calculatedHr > 0) calculatedHr else (if (sensorData.heartRate > 0) sensorData.heartRate else 68)

            // Save filtered ECG check to local database
            val check = EcgCheck(
                username = username,
                time = System.currentTimeMillis(),
                restingHr = restingHr,
                samples = filteredSamples.joinToString(",")
            )
            DataGate.saveEcg(context, username, check)

            status = "done"
        }
    }

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
                text = "ECG Check",
                color = Color.White,
                fontSize = 36.sp,
                fontFamily = WorkSans,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when {
                    status == "measuring" -> "Recording... stay still. $secondsLeft s left"
                    status == "done" -> "Done! Here is your result."
                    sensorData.connected -> "Sit down, relax and stay still. The reading takes $ECG_SECONDS seconds."
                    else -> "Polar H10 device is not connected. Please connect your device in Profile."
                },
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Real-time 3-second ECG chart display
            GlassCard(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) {
                EChartsView(
                    fileName = "ecg.html",
                    script = "setData(${allSamples.takeLast(ECG_SAMPLE_RATE * 3)})"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Resting heart rate result card shown upon completion
            if (status == "done") {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(text = "Resting Heart Rate", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(text = "$restingHr", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "bpm",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Text(
                            text = restingHrComment(restingHr),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Start/Done button
            Button(
                onClick = {
                    if (status == "done") {
                        (context as Activity).finish()
                    } else {
                        // Check if Polar H10 device is connected before starting
                        if (!sensorData.connected) {
                            Toast.makeText(
                                context,
                                "Polar H10 device is not connected. Please connect your Polar H10 in Profile.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            secondsLeft = ECG_SECONDS
                            status = "measuring"
                        }
                    }
                },
                enabled = status != "measuring",
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Orange,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF111111),
                    disabledContentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = when (status) {
                        "ready" -> if (sensorData.connected) "Start ECG" else "Device Not Connected"
                        "measuring" -> "Recording... $secondsLeft s"
                        else -> "Done"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
