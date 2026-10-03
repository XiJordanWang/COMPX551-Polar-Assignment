package com.example.polar.ui.page

import android.os.Bundle
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.polar.data.DataGate
import com.example.polar.data.db.AppDatabase
import com.example.polar.data.entity.Workout
import com.example.polar.data.entity.heartRateList
import com.example.polar.data.model.emojiFor
import com.example.polar.data.online.Assessment
import com.example.polar.logic.caloriesBurned
import com.example.polar.logic.formatDuration
import com.example.polar.ui.theme.FieldGrey
import com.example.polar.ui.theme.PolarTheme
import com.example.polar.ui.theme.WorkSans
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkoutDetailPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Sent from the workout list on the history tab
        val workoutId = intent.getLongExtra("workoutId", 0)

        setContent {
            PolarTheme {
                WorkoutDetailScreen(workoutId)
            }
        }
    }
}

@Composable
fun WorkoutDetailScreen(workoutId: Long) {
    val context = LocalContext.current
    var workout by remember { mutableStateOf<Workout?>(null) }
    var assessment by remember { mutableStateOf<Assessment?>(null) }

    LaunchedEffect(Unit) {
        val db = AppDatabase.getDatabase(context)
        val found = db.workoutDao().findById(workoutId)
        workout = found
        if (found != null) {
            // Calories need the assessment (local or online based on privacy mode)
            assessment = DataGate.loadAssessment(context, found.username)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Still loading from the database
        val w = workout ?: return@Column

        val dateFormat = SimpleDateFormat("EEEE d MMM yyyy, h:mm a", Locale.ENGLISH)
        val calories = assessment?.let { "${caloriesBurned(it, w.avgHr, w.durationSec)}" } ?: "--"

        Text(
            text = "${emojiFor(w.type)} ${w.type}",
            color = Color.Black,
            fontSize = 34.sp,
            fontFamily = WorkSans,
            fontWeight = FontWeight.Bold
        )
        Text(text = dateFormat.format(Date(w.startTime)), color = Color.DarkGray, fontSize = 15.sp)

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ResultBox(label = "Duration", value = formatDuration(w.durationSec), note = "", modifier = Modifier.weight(1f))
            ResultBox(label = "Calories", value = calories, note = "kcal", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ResultBox(label = "Min", value = "${w.minHr}", note = "bpm", modifier = Modifier.weight(1f))
            ResultBox(label = "Avg", value = "${w.avgHr}", note = "bpm", modifier = Modifier.weight(1f))
            ResultBox(label = "Max", value = "${w.maxHr}", note = "bpm", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(20.dp))

        FieldLabel("Heart rate (pinch or drag the slider to zoom)")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(FieldGrey, RoundedCornerShape(20.dp))
                .padding(8.dp)
        ) {
            EChartsView(fileName = "history_chart.html", script = "setData(${w.heartRateList()})")
        }
    }
}
