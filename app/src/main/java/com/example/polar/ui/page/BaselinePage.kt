package com.example.polar.ui.page

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.example.polar.ui.theme.Orange
import com.example.polar.ui.theme.PolarTheme
import com.example.polar.ui.theme.WorkSans
import kotlinx.coroutines.delay
import com.example.polar.data.polar.PolarManager
import androidx.compose.runtime.collectAsState
import com.example.polar.data.DataGate
import com.example.polar.data.db.AppDatabase
import com.example.polar.data.entity.Baseline
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

// Duration of the baseline measurement
private const val BASELINE_SECONDS = 30

class BaselinePage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val username = intent.getStringExtra("username") ?: "demo"
        setContent {
            PolarTheme {
                BaselineScreen(username)
            }
        }
    }
}

@Composable
fun BaselineScreen(username: String = "demo") {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val polarManager = remember {
        PolarManager(context)
    }
    val db = remember {
        AppDatabase.getDatabase(context)
    }
    val deviceIdFlow = remember {
        db.deviceDao().observeDeviceId(username)
    }
    val deviceId by deviceIdFlow.collectAsState(initial = null)

    LaunchedEffect(deviceId) {
        if (!deviceId.isNullOrBlank()) {
            polarManager.connect(deviceId!!)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            polarManager.disconnect(deviceId)
        }
    }

    var restingHr by remember {
        mutableIntStateOf(0)
    }

    val sensorData by polarManager.sensorData.collectAsState()

    // "ready" -> "measuring" -> "done"
    var status by remember { mutableStateOf("ready") }
    var secondsLeft by remember { mutableIntStateOf(BASELINE_SECONDS) }
    var stable by remember { mutableStateOf(true) }

    var variation by remember {
        mutableIntStateOf(0)
    }
    // Heart-rate samples used to calculate resting baseline
    val baselineValues = remember {
        mutableStateListOf<Int>()
    }


    LaunchedEffect(status) {

        if (status == "measuring") {

            baselineValues.clear()

            for (second in 1..BASELINE_SECONDS) {

                delay(1000)

                secondsLeft = BASELINE_SECONDS - second

                // Ignore first 5 seconds
                if (second > 5 && sensorData.heartRate > 0) {
                    baselineValues.add(
                        sensorData.heartRate
                    )
                }
            }

            restingHr =
                if (baselineValues.isEmpty()) {
                    0
                } else {
                    baselineValues.average().toInt()
                }

            variation =
                if (baselineValues.isEmpty()) {
                    0
                } else {
                    (baselineValues.maxOrNull() ?: 0) -
                            (baselineValues.minOrNull() ?: 0)
                }

            stable = variation < 10

            scope.launch {
                DataGate.saveBaseline(
                    context,
                    username,
                    Baseline(
                        username = username,
                        baselineHr = restingHr
                    )
                )
            }

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
                text = "Baseline Check",
                color = Color.White,
                fontSize = 36.sp,
                fontFamily = WorkSans,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when (status) {
                    "ready" ->
                        "Sit relaxed and remain still. We will measure your resting heart rate baseline over 30 seconds."

                    "measuring" ->
                        "Recording... stay still. $secondsLeft s left"

                    else ->
                        "Baseline measurement completed."
                },
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Live heart-rate display
            GlassCard(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = "${sensorData.heartRate}",
                        color = Color.White,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "bpm",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Result is only shown after the reading is finished
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
                            text = if (stable) {
                                "Baseline is stable."
                            } else {
                                "Baseline unstable. Please sit still and retry."
                            },
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Variation: $variation bpm",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    if (status == "done") {
                        (context as Activity).finish()
                    } else {
                        secondsLeft = BASELINE_SECONDS
                        status = "measuring"
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
                        "ready" -> "Start Baseline"
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
