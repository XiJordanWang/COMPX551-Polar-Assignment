package com.example.polar.ui.page

/** Tab displaying past workouts, activity charts, personal bests, and workout statistics. */

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.polar.data.db.AppDatabase
import com.example.polar.data.entity.EcgCheck
import com.example.polar.data.entity.Workout
import com.example.polar.data.model.emojiFor
import com.example.polar.data.online.Assessment
import com.example.polar.logic.DayStats
import com.example.polar.logic.PersonalBests
import com.example.polar.logic.StatDelta
import com.example.polar.logic.WeekOverWeekDelta
import com.example.polar.logic.WeekStats
import com.example.polar.logic.calculatePersonalBests
import com.example.polar.logic.dailyStats
import com.example.polar.logic.dayLabels
import com.example.polar.logic.formatDuration
import com.example.polar.logic.maxHeartRate
import com.example.polar.logic.restingHrComment
import com.example.polar.logic.rollingMean7Day
import com.example.polar.logic.streakDays
import com.example.polar.logic.weekOverWeekDelta
import com.example.polar.logic.weeklyStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HistoryUiState(
    val isLoading: Boolean = true,
    val workouts: List<Workout> = emptyList(),
    val ecgChecks: List<EcgCheck> = emptyList(),
    val weekOverWeek: WeekOverWeekDelta? = null,
    val dailyStats: List<DayStats> = emptyList(),
    val rollingMean7Day: List<Double> = emptyList(),
    val weeklyStats: List<WeekStats> = emptyList(),
    val currentStreak: Int = 0,
    val personalBests: PersonalBests = PersonalBests()
)

class HistoryViewModel(
    private val db: AppDatabase,
    private val username: String,
    private val maxHr: Int = 200
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                db.workoutDao().getWorkouts(username),
                db.ecgDao().getChecks(username)
            ) { workouts, ecgChecks ->
                Pair(workouts, ecgChecks)
            }
                .flowOn(Dispatchers.Default)
                .collect { (workouts, ecgChecks) ->
                    withContext(Dispatchers.Default) {
                        val wow = if (workouts.isNotEmpty()) weekOverWeekDelta(workouts, maxHr) else null
                        val last7Days = dailyStats(workouts, 7, maxHr)
                        val rollingMean = rollingMean7Day(last7Days)
                        val weeks = weeklyStats(workouts, 8, maxHr)
                        val streak = streakDays(workouts)
                        val bests = calculatePersonalBests(workouts, ecgChecks, maxHr)

                        _uiState.value = HistoryUiState(
                            isLoading = false,
                            workouts = workouts,
                            ecgChecks = ecgChecks,
                            weekOverWeek = wow,
                            dailyStats = last7Days,
                            rollingMean7Day = rollingMean,
                            weeklyStats = weeks,
                            currentStreak = streak,
                            personalBests = bests
                        )
                    }
                }
        }
    }
}

class HistoryViewModelFactory(
    private val db: AppDatabase,
    private val username: String,
    private val maxHr: Int = 200
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HistoryViewModel(db, username, maxHr) as T
    }
}

@Composable
fun HistoryContent(username: String, assessment: Assessment?) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val maxHr = if (assessment != null) maxHeartRate(assessment.age) else 200

    val viewModel = remember(username) {
        HistoryViewModelFactory(db, username, maxHr).create(HistoryViewModel::class.java)
    }
    val uiState by viewModel.uiState.collectAsState()

    var subTab by remember { mutableStateOf("overview") } // "overview", "trends", "sessions"

    Column {
        Spacer(modifier = Modifier.height(8.dp))

        // Sub-tab selection bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SubTabButton(
                text = "Overview",
                selected = subTab == "overview",
                onClick = { subTab = "overview" },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            SubTabButton(
                text = "Trends",
                selected = subTab == "trends",
                onClick = { subTab = "trends" },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            SubTabButton(
                text = "Sessions",
                selected = subTab == "sessions",
                onClick = { subTab = "sessions" },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (subTab) {
            "overview" -> OverviewTabContent(uiState)
            "trends" -> TrendsTabContent(uiState)
            "sessions" -> SessionsTabContent(uiState, assessment)
        }
    }
}

@Composable
fun SubTabButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.Black else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ---------- Sub-tab 1: Overview ----------

@Composable
fun OverviewTabContent(uiState: HistoryUiState) {
    if (uiState.workouts.isEmpty()) {
        EmptyCard(
            title = "No workouts yet",
            subtitle = "Complete a workout to see your weekly comparison, daily trends, and personal bests!"
        )
        return
    }

    WeekOverWeekCard(uiState)
    Spacer(modifier = Modifier.height(12.dp))
    DailyBarsCard(uiState)
    Spacer(modifier = Modifier.height(12.dp))
    StreakCard(uiState.currentStreak)
    Spacer(modifier = Modifier.height(12.dp))
    PersonalBestsCard(uiState.personalBests)
}

@Composable
fun WeekOverWeekCard(uiState: HistoryUiState) {
    val wow = uiState.weekOverWeek
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            CardTitle(emoji = "📊", title = "This Week vs Last Week")
            Spacer(modifier = Modifier.height(12.dp))

            if (wow == null) {
                Text(text = "Not enough workout data yet for weekly comparison.", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricDeltaCol(label = "Minutes", delta = wow.minutes, unit = "m", modifier = Modifier.weight(1f))
                    MetricDeltaCol(label = "Points", delta = wow.points, unit = "pts", modifier = Modifier.weight(1f))
                    MetricDeltaCol(label = "Sessions", delta = wow.sessions, unit = "", modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun MetricDeltaCol(label: String, delta: StatDelta, unit: String, modifier: Modifier = Modifier) {
    val isUp = delta.difference >= 0
    val arrow = if (isUp) "↑" else "↓"
    val color = if (isUp) Color(0xFF6EE7B7) else Color(0xFFFCA5A5) // Soft green / soft red
    val signStr = if (isUp) "+" else ""

    Column(modifier = modifier) {
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
        Text(
            text = "${delta.currentValue.toInt()}$unit",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$arrow $signStr${delta.percentChange.toInt()}%",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun DailyBarsCard(uiState: HistoryUiState) {
    val labels = remember { dayLabels(7, "EEE") }
    val minutes = uiState.dailyStats.map { it.minutes }
    val rollingMean = uiState.rollingMean7Day

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            CardTitle(emoji = "🔥", title = "Daily Minutes & 7-Day Mean")
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)) {
                EChartsView(
                    fileName = "week_bars.html",
                    script = "setData(${toJsStrings(labels)}, $minutes, $rollingMean)"
                )
            }
        }
    }
}

@Composable
fun StreakCard(streak: Int) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🔥", fontSize = 36.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "Current Streak", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Text(
                    text = "$streak ${if (streak == 1) "day" else "days"}",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Days meeting your 30-min workout goal",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun PersonalBestsCard(bests: PersonalBests) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            CardTitle(emoji = "🏆", title = "Personal Bests")
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                BestItem(
                    label = "Longest Session",
                    value = if (bests.longestSession.durationSec > 0) formatDuration(bests.longestSession.durationSec) else "--",
                    subValue = bests.longestSession.date,
                    modifier = Modifier.weight(1f)
                )
                BestItem(
                    label = "Most Points Session",
                    value = if (bests.mostPointsSession.points > 0) "${bests.mostPointsSession.points} pts" else "--",
                    subValue = bests.mostPointsSession.date,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                BestItem(
                    label = "Longest Streak Ever",
                    value = if (bests.longestStreakEver > 0) "${bests.longestStreakEver} ${if (bests.longestStreakEver == 1) "day" else "days"}" else "--",
                    subValue = if (bests.longestStreakEver > 0) "Goal-met streak record" else "",
                    modifier = Modifier.weight(1f)
                )
                BestItem(
                    label = "Lowest Resting HR",
                    value = if (bests.lowestRestingHr.restingHr > 0) "${bests.lowestRestingHr.restingHr} bpm" else "--",
                    subValue = bests.lowestRestingHr.date,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun BestItem(label: String, value: String, subValue: String = "", modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
        Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (subValue.isNotEmpty()) {
            Text(text = subValue, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}

// ---------- Sub-tab 2: Trends ----------

@Composable
fun TrendsTabContent(uiState: HistoryUiState) {
    EcgTrendsCard(uiState.ecgChecks)
    Spacer(modifier = Modifier.height(12.dp))
    WeeklyTrendsCard(uiState)
}

@Composable
fun EcgTrendsCard(ecgChecks: List<EcgCheck>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            CardTitle(emoji = "🫀", title = "Resting Heart Rate Over Time")
            Spacer(modifier = Modifier.height(8.dp))

            if (ecgChecks.isEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No ECG checks yet",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Record a 30s ECG check on the home screen to start tracking resting heart rate trends.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            } else {
                val dateFormat = SimpleDateFormat("d MMM", Locale.ENGLISH)
                // Ordered chronologically for the line chart (oldest to newest)
                val sorted = ecgChecks.sortedBy { it.time }
                val labels = sorted.map { dateFormat.format(Date(it.time)) }
                val values = sorted.map { it.restingHr }

                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)) {
                    EChartsView(
                        fileName = "trends_chart.html",
                        script = "setData(${toJsStrings(labels)}, $values, 'bpm')"
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyTrendsCard(uiState: HistoryUiState) {
    val weeklyStats = uiState.weeklyStats
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            CardTitle(emoji = "📅", title = "Weekly Activity")
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.workouts.isEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "No workouts yet", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Complete workouts to unlock weekly minutes and points trend analysis.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            } else {
                val labels = weeklyStats.map { it.weekLabel }
                val minutes = weeklyStats.map { it.minutes }

                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)) {
                    EChartsView(
                        fileName = "trends_chart.html",
                        script = "setData(${toJsStrings(labels)}, $minutes, 'min')"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                for (week in weeklyStats.takeLast(4).reversed()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = week.weekLabel, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(text = "${week.minutes.toInt()} min  ·  ${week.points} pts", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// ---------- Sub-tab 3: Sessions ----------

@Composable
fun SessionsTabContent(uiState: HistoryUiState, assessment: Assessment?) {
    WorkoutListCard(uiState.workouts)
    Spacer(modifier = Modifier.height(12.dp))
    EcgCheckListCard(uiState.ecgChecks)
}

@Composable
fun WorkoutListCard(workouts: List<Workout>) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("d MMM, h:mm a", Locale.ENGLISH)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            CardTitle(emoji = "🏋️", title = "Past Workouts")
            Spacer(modifier = Modifier.height(8.dp))

            if (workouts.isEmpty()) {
                Text(text = "No workouts yet", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Finish a workout and it will show up here.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            } else {
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
}

@Composable
fun EcgCheckListCard(ecgChecks: List<EcgCheck>) {
    val dateFormat = SimpleDateFormat("d MMM, h:mm a", Locale.ENGLISH)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            CardTitle(emoji = "🫀", title = "Past ECG Checks")
            Spacer(modifier = Modifier.height(8.dp))

            if (ecgChecks.isEmpty()) {
                Text(text = "No ECG checks yet", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Take a 30 second reading from the Home tab.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            } else {
                for (check in ecgChecks) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💓", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${check.restingHr} bpm",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = dateFormat.format(Date(check.time)),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = restingHrComment(check.restingHr),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCard(title: String, subtitle: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(text = title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
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
