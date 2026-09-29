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
import com.example.polar.logic.ECG_SAMPLE_RATE
import com.example.polar.logic.fakeEcgValue
import com.example.polar.logic.heartRateFromEcg
import com.example.polar.logic.restingHrComment
import com.example.polar.ui.theme.Orange
import com.example.polar.ui.theme.PolarTheme
import com.example.polar.ui.theme.WorkSans
import kotlinx.coroutines.delay
import kotlin.random.Random

// How long one ECG reading takes
const val ECG_SECONDS = 30

class EcgPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PolarTheme {
                EcgScreen()
            }
        }
    }
}

@Composable
fun EcgScreen() {
    val context = LocalContext.current

    // "ready" -> "measuring" -> "done"
    var status by remember { mutableStateOf("ready") }
    var secondsLeft by remember { mutableIntStateOf(ECG_SECONDS) }
    var restingHr by remember { mutableIntStateOf(0) }
    // Every sample of this reading (30s x 130 = 3900 samples)
    val allSamples = remember { mutableStateListOf<Int>() }

    // Runs every time status changes. Only does work when measuring.
    LaunchedEffect(status) {
        if (status == "measuring") {
            allSamples.clear()
            // TODO: replace this fake ECG with the real ECG stream from the Polar SDK
            val fakeHr = Random.nextInt(58, 72)
            var phase = 0.0 // where we are inside one heart beat, 0.0 to 1.0

            // 10 times a second, add 13 samples (= 130 per second)
            for (tick in 1..ECG_SECONDS * 10) {
                delay(100)
                for (i in 0 until 13) {
                    phase += (fakeHr / 60.0) / ECG_SAMPLE_RATE
                    if (phase >= 1.0) phase -= 1.0
                    allSamples.add(fakeEcgValue(phase))
                }
                secondsLeft = ECG_SECONDS - tick / 10
            }

            restingHr = heartRateFromEcg(allSamples)
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
                text = when (status) {
                    "ready" -> "Sit down, relax and stay still. The reading takes $ECG_SECONDS seconds."
                    "measuring" -> "Recording... stay still. $secondsLeft s left"
                    else -> "Done! Here is your result."
                },
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ECG chart, shows the last 3 seconds
            GlassCard(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) {
                EChartsView(
                    fileName = "ecg.html",
                    script = "setData(${allSamples.takeLast(ECG_SAMPLE_RATE * 3)})"
                )
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
                            text = restingHrComment(restingHr),
                            color = Color.White.copy(alpha = 0.85f),
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
                        secondsLeft = ECG_SECONDS
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
                        "ready" -> "Start ECG"
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
