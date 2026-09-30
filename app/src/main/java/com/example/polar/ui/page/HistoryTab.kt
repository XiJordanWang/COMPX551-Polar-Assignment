package com.example.polar.ui.page

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.polar.data.entity.Workout
import com.example.polar.data.entity.heartRateList
import com.example.polar.data.model.emojiFor
import com.example.polar.data.online.Assessment
import com.example.polar.logic.caloriesBurned
import com.example.polar.logic.dayLabels
import com.example.polar.logic.formatDuration
import com.example.polar.logic.minutesPerDay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HistoryContent(workouts: List<Workout>, assessment: Assessment?) {
    Spacer(modifier = Modifier.height(8.dp))

    if (workouts.isEmpty()) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(text = "No workouts yet", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Finish a workout and it will show up here.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp
                )
            }
        }
        return
    }

    // Newest workout is first because the query sorts by startTime DESC
    val latest = workouts.first()

    WorkoutTimeCard(workouts)
    Spacer(modifier = Modifier.height(12.dp))
    HeartRateRangeCard(latest)
    Spacer(modifier = Modifier.height(12.dp))
    LastWorkoutCard(latest, assessment)
    Spacer(modifier = Modifier.height(12.dp))
    WorkoutListCard(workouts)
}

// Bar chart of minutes per day, for the last week or month
@Composable
fun WorkoutTimeCard(workouts: List<Workout>) {
    // "W" = last 7 days, "M" = last 30 days
    var range by remember { mutableStateOf("W") }
    val days = if (range == "W") 7 else 30

    val minutes = minutesPerDay(workouts, days)
    val labels = dayLabels(days, if (range == "W") "EEE" else "d")
    val average = (minutes.sum() / days * 10).roundToInt() / 10.0

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CardTitle(emoji = "🔥", title = "Workouts", modifier = Modifier.weight(1f))
                RangeButton(text = "W", selected = range == "W", onClick = { range = "W" })
                Spacer(modifier = Modifier.width(6.dp))
                RangeButton(text = "M", selected = range == "M", onClick = { range = "M" })
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You worked out for an average of $average min a day in the last $days days.",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)) {
                EChartsView(
                    fileName = "week_bars.html",
                    script = "setData(${toJsStrings(labels)}, $minutes, $average)"
                )
            }
        }
    }
}

// Floating bars showing the lowest and highest heart rate in each part of the last workout
@Composable
fun HeartRateRangeCard(workout: Workout) {
    val heartRates = workout.heartRateList()
    // Split the workout into about 20 parts
    val partSize = maxOf(1, (heartRates.size + 19) / 20)
    val parts = heartRates.chunked(partSize)
    val mins = parts.map { it.min() }
    val maxs = parts.map { it.max() }

    val timeFormat = SimpleDateFormat("h:mm a", Locale.ENGLISH)
    val start = timeFormat.format(Date(workout.startTime))
    val end = timeFormat.format(Date(workout.startTime + workout.durationSec * 1000L))

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            CardTitle(emoji = "❤️", title = "Heart Rate: Workout")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your heart rate range during your recent workout was ${workout.minHr}–${workout.maxHr} beats per minute.",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)) {
                EChartsView(fileName = "hr_range.html", script = "setData($mins, $maxs)")
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = start, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(text = end, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun LastWorkoutCard(workout: Workout, assessment: Assessment?) {
    // Calories need gender, age and weight from the assessment
    val calories = if (assessment != null) {
        "${caloriesBurned(assessment, workout.avgHr, workout.durationSec)} kcal"
    } else {
        "-- kcal"
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            CardTitle(emoji = "⭐", title = "Last Workout")
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = emojiFor(workout.type), fontSize = 40.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = workout.type, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Text(
                        text = "${formatDuration(workout.durationSec)}  ·  $calories",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = "Average ${workout.avgHr} bpm", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                }
            }
            if (assessment == null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Do the assessment to see calories burned.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

// Every workout, tap one to see the full heart rate chart
@Composable
fun WorkoutListCard(workouts: List<Workout>) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("d MMM, h:mm a", Locale.ENGLISH)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            CardTitle(emoji = "📅", title = "All Workouts")
            Spacer(modifier = Modifier.height(4.dp))
            for (workout in workouts) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(context, WorkoutDetailPage::class.java)
                            intent.putExtra("workoutId", workout.id)
                            context.startActivity(intent)
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = emojiFor(workout.type), fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = workout.type, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = dateFormat.format(Date(workout.startTime)),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = formatDuration(workout.durationSec), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "${workout.avgHr} bpm", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "›", color = Color.White, fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
fun CardTitle(emoji: String, title: String, modifier: Modifier = Modifier) {
    Text(
        text = "$emoji $title",
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@Composable
fun RangeButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.2f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.Black else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
