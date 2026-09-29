package com.example.polar.ui.page

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.polar.data.db.AppDatabase
import com.example.polar.data.entity.Assessment
import com.example.polar.data.entity.Workout
import com.example.polar.data.model.WorkoutType
import com.example.polar.data.model.emojiFor
import com.example.polar.data.model.workoutTypes
import com.example.polar.logic.formatDuration
import com.example.polar.logic.maxHeartRate
import com.example.polar.logic.streakDays
import com.example.polar.logic.todayPoints
import com.example.polar.logic.totalPoints
import com.example.polar.ui.theme.FieldGrey
import com.example.polar.ui.theme.Orange
import com.example.polar.ui.theme.PolarTheme
import com.example.polar.ui.theme.WorkSans

class MainPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Sent from SignPage after a successful sign in
        val firstName = intent.getStringExtra("firstName") ?: ""
        val lastName = intent.getStringExtra("lastName") ?: ""
        val username = intent.getStringExtra("username") ?: ""

        setContent {
            PolarTheme {
                SensorScreen(firstName, lastName, username)
            }
        }
    }
}

@Composable
fun SensorScreen(firstName: String, lastName: String, username: String) {
    // Which tab is showing: "home", "history" or "profile"
    var tab by remember { mutableStateOf("home") }

    // Read workouts and assessment from Room. Because they are Flows,
    // the screen updates by itself after a workout is saved.
    // remember {} so we don't create a new Flow every recomposition.
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val workoutsFlow = remember { db.workoutDao().getWorkouts(username) }
    val assessmentFlow = remember { db.assessmentDao().observe(username) }
    val workouts by workoutsFlow.collectAsState(initial = emptyList())
    val assessment by assessmentFlow.collectAsState(initial = null)

    val title = when (tab) {
        "history" -> "History"
        "profile" -> "Profile"
        else -> "My Plant"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GardenBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                firstName = firstName,
                title = title,
                initials = firstName.take(1) + lastName.take(1),
                onAvatarClick = { tab = "profile" }
            )

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                when (tab) {
                    "home" -> HomeContent(username, workouts, assessment)
                    "history" -> HistoryContent(workouts, assessment)
                    "profile" -> ProfileContent(firstName, lastName, username)
                }
                // Space so the bottom bar doesn't cover the last card
                Spacer(modifier = Modifier.height(130.dp))
            }
        }

        BottomBar(
            tab = tab,
            username = username,
            onTabClick = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun Header(firstName: String, title: String, initials: String, onAvatarClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hello, $firstName",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 18.sp,
                fontFamily = WorkSans,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 36.sp,
                fontFamily = WorkSans,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp // Slightly tighter tracking, like the reference design
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Avatar with the user's initials, tap it to open the profile
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Orange)
                .border(2.dp, Color.White, CircleShape)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials.uppercase(),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// See-through white card that looks good on the garden background
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        content()
    }
}

@Composable
fun HomeContent(username: String, workouts: List<Workout>, assessment: Assessment?) {
    val context = LocalContext.current

    // Zones need max heart rate. Use 220 - age if the user did the assessment.
    val maxHr = if (assessment != null) maxHeartRate(assessment.age) else 200
    // Only recalculate when the workouts or the assessment change
    val points = remember(workouts, maxHr) { totalPoints(workouts, maxHr) }
    val today = remember(workouts, maxHr) { todayPoints(workouts, maxHr) }
    val streak = remember(workouts) { streakDays(workouts) }

    Spacer(modifier = Modifier.height(8.dp))

    PlantCard(points = points)

    Spacer(modifier = Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(label = "🔥 Streak", value = "$streak", unit = if (streak == 1) "day" else "days", modifier = Modifier.weight(1f))
        StatCard(label = "⭐ Today", value = "$today", unit = "pts", modifier = Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(12.dp))

    val last = workouts.firstOrNull()
    if (last == null) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(text = "Last Workout", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No workouts yet. Tap the orange button to start one!",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp
                )
            }
        }
    } else {
        ModuleCard(
            emoji = emojiFor(last.type),
            title = "Last Workout: ${last.type}",
            subtitle = "${formatDuration(last.durationSec)}  ·  avg ${last.avgHr} bpm"
        ) {
            val intent = Intent(context, WorkoutDetailPage::class.java)
            intent.putExtra("workoutId", last.id)
            context.startActivity(intent)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    ModuleCard(emoji = "❤️", title = "ECG Check", subtitle = "30 second reading at rest") {
        context.startActivity(Intent(context, EcgPage::class.java))
    }

    Spacer(modifier = Modifier.height(12.dp))

    ModuleCard(emoji = "📋", title = "Assessment", subtitle = "Set your personal heart rate zones") {
        val intent = Intent(context, AssessmentPage::class.java)
        intent.putExtra("username", username)
        context.startActivity(intent)
    }
}

// Card that opens another page, like "ECG Check" or "Assessment"
@Composable
fun ModuleCard(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    GlassCard(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
            }
            Text(text = "›", color = Color.White, fontSize = 32.sp)
        }
    }
}

@Composable
fun StatCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Column {
            Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileContent(firstName: String, lastName: String, username: String) {
    val context = LocalContext.current

    Spacer(modifier = Modifier.height(8.dp))

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(text = "Name", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Text(text = "$firstName $lastName", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Username", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Text(text = username, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = {
            // Go back to the sign in page and close this one
            context.startActivity(Intent(context, SignPage::class.java))
            (context as Activity).finish()
        },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111111), contentColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text(text = "Sign Out", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun BottomBar(tab: String, username: String, onTabClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Show the "choose workout" popup when the big button is pressed
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        WorkoutPicker(
            onPick = { type ->
                showPicker = false
                val intent = Intent(context, WorkoutPage::class.java)
                intent.putExtra("workoutType", type.name)
                intent.putExtra("username", username)
                context.startActivity(intent)
            },
            onDismiss = { showPicker = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // White pill with Home on the left and History on the right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(Color.White),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = Icons.Filled.Home,
                label = "Home",
                selected = tab == "home",
                onClick = { onTabClick("home") },
                modifier = Modifier.weight(1f)
            )
            // Empty space in the middle for the big button
            Spacer(modifier = Modifier.width(88.dp))
            NavItem(
                icon = Icons.Filled.DateRange,
                label = "History",
                selected = tab == "history",
                onClick = { onTabClick("history") },
                modifier = Modifier.weight(1f)
            )
        }

        // Big orange button to start a new workout, sits a bit above the bar
        Box(
            modifier = Modifier
                .offset(y = (-20).dp)
                .size(76.dp)
                .clip(CircleShape)
                .background(Orange)
                .border(4.dp, Color.White, CircleShape)
                .clickable { showPicker = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Start new workout",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Selected tab is orange, the others are grey
    val color = if (selected) Orange else Color.Gray

    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color)
        Text(text = label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// Popup with all the workout types in two columns
@Composable
fun WorkoutPicker(onPick: (WorkoutType) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(20.dp)
        ) {
            Text(
                text = "Choose Workout",
                color = Color.Black,
                fontSize = 24.sp,
                fontFamily = WorkSans,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // chunked(2) splits the list into rows of 2
            for (row in workoutTypes.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (type in row) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(FieldGrey)
                                .clickable { onPick(type) }
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = type.emoji, fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = type.name,
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Cancel", color = Orange, fontSize = 16.sp)
            }
        }
    }
}
