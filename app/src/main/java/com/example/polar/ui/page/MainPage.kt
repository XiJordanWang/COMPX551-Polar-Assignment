package com.example.polar.ui.page

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.polar.data.db.AppDatabase
import com.example.polar.data.entity.Device
import com.example.polar.data.entity.Workout
import com.example.polar.data.entity.heartRateList
import com.example.polar.data.model.WorkoutType
import com.example.polar.data.model.emojiFor
import com.example.polar.data.model.workoutTypes
import com.example.polar.data.online.Assessment
import com.example.polar.data.online.AssessmentTable
import com.example.polar.data.online.WorkoutSummary
import com.example.polar.data.online.WorkoutSummaryTable
import com.example.polar.data.prefs.SessionStore
import com.example.polar.logic.cleanDeviceIdInput
import com.example.polar.logic.demoWorkouts
import com.example.polar.logic.formatDuration
import com.example.polar.logic.isValidDeviceId
import com.example.polar.logic.streakDays
import com.example.polar.logic.todayPoints
import com.example.polar.logic.totalPoints
import com.example.polar.logic.PointsCalculator
import com.example.polar.ui.theme.FieldGrey
import com.example.polar.ui.theme.Orange
import com.example.polar.ui.theme.PolarTheme
import com.example.polar.ui.theme.WorkSans
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                SensorScreen(firstName, lastName, username, resumeCount)
            }
        }
    }

    // Goes up by 1 every time this page comes back to the front,
    // e.g. after the Assessment page. The screen uses it to reload online data.
    private var resumeCount by mutableIntStateOf(0)

    override fun onResume() {
        super.onResume()
        resumeCount++
    }
}

@Composable
fun SensorScreen(firstName: String, lastName: String, username: String, resumeCount: Int) {
    // Which tab is showing: "home", "history" or "profile"
    var tab by remember { mutableStateOf("home") }

    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(context, "Bluetooth permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    val requestPermissionsIfNeeded = {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        val hasPermissions = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermissions) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    LaunchedEffect(Unit) {
        requestPermissionsIfNeeded()
    }

    // Local data (Room): workouts and the device ID. Because they are Flows,
    // the screen updates by itself when they change.
    // remember {} so we don't create a new Flow every recomposition.
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val workoutsFlow = remember { db.workoutDao().getWorkouts(username) }
    val deviceIdFlow = remember { db.deviceDao().observeDeviceId(username) }
    val workouts by workoutsFlow.collectAsState(initial = emptyList())
    val deviceId by deviceIdFlow.collectAsState(initial = null)

    // Online data (Supabase): the assessment. Online tables don't update by themselves,
    // so we load it again every time this page comes back (resumeCount changes).
    var assessment by remember { mutableStateOf<Assessment?>(null) }
    LaunchedEffect(resumeCount) {
        assessment = AssessmentTable.findByUsername(username)
    }

    val title = when (tab) {
        "history" -> "History"
        "social" -> "Plant Friends"
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
                    "home" -> HomeContent(
                        username = username,
                        workouts = workouts,
                        deviceId = deviceId,
                        onDeviceClick = { tab = "profile" }
                    )
                    "history" -> HistoryContent(username, assessment)
                    "social" -> SocialContent(username)
                    "profile" -> ProfileContent(
                        firstName = firstName,
                        lastName = lastName,
                        username = username,
                        deviceId = deviceId
                    )
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
fun HomeContent(
    username: String,
    workouts: List<Workout>,
    deviceId: String?,
    onDeviceClick: () -> Unit
) {
    val context = LocalContext.current

    // Points compare heart rate with the resting baseline (see PointsCalculator).
    // Only recalculate when the workouts change.
    val baseline = PointsCalculator.DEFAULT_BASELINE_HR
    val points = remember(workouts) { totalPoints(workouts, baseline) }
    val today = remember(workouts) { todayPoints(workouts, baseline) }
    val streak = remember(workouts) { streakDays(workouts) }

    Spacer(modifier = Modifier.height(8.dp))

    PlantCard(points = points)

    Spacer(modifier = Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(label = "🔥 Streak", value = "$streak", unit = if (streak == 1) "day" else "days", modifier = Modifier.weight(1f))
        StatCard(label = "⭐ Today", value = "$today", unit = "pts", modifier = Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Which Polar H10 we connect to. Tapping it opens the profile tab to add or change it.
    ModuleCard(
        emoji = "💓",
        title = "My Polar H10",
        subtitle = if (deviceId == null) "Not set · tap to add your device ID" else "Device ID: $deviceId",
        onClick = onDeviceClick
    )

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
        val intent = Intent(context, EcgPage::class.java)
        intent.putExtra("username", username)
        context.startActivity(intent)
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
fun ProfileContent(
    firstName: String,
    lastName: String,
    username: String,
    deviceId: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val workouts = db.workoutDao().getWorkouts(username).first()
                    val ecgChecks = db.ecgDao().getChecks(username).first()

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
                    val sb = StringBuilder()

                    sb.append("=== WORKOUT SESSIONS ===\n")
                    sb.append("ID,Type,Start Time,Duration (sec),Min HR,Avg HR,Max HR,Heart Rates\n")
                    for (w in workouts) {
                        val dateStr = dateFormat.format(Date(w.startTime))
                        val hrString = "\"${w.heartRates}\""
                        sb.append("${w.id},\"${w.type}\",$dateStr,${w.durationSec},${w.minHr},${w.avgHr},${w.maxHr},$hrString\n")
                    }

                    sb.append("\n=== ECG CHECKS ===\n")
                    sb.append("ID,Time,Resting HR,Samples\n")
                    for (ecg in ecgChecks) {
                        val dateStr = dateFormat.format(Date(ecg.time))
                        val sampleString = "\"${ecg.samples}\""
                        sb.append("${ecg.id},$dateStr,${ecg.restingHr},$sampleString\n")
                    }

                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(sb.toString().toByteArray(Charsets.UTF_8))
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Data exported successfully", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

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

    Spacer(modifier = Modifier.height(12.dp))

    DeviceIdCard(username = username, savedId = deviceId)

    //setting button
    Button(
        onClick = {
            val intent = Intent(
                context,
                SettingsPage::class.java
            )

            intent.putExtra(
                "username",
                username
            )

            context.startActivity(intent)
        },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Orange,
            contentColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Spacer(modifier = Modifier.height(12.dp))

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(text = "📁 Export Data", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Export your workout sessions and ECG checks as a CSV file.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
                    exportLauncher.launch("polar_data_$timestamp.csv")
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(text = "Export My Data (CSV)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Only in debug builds (when running from Android Studio), never in a release app
    if (BuildConfig.DEBUG) {
        Spacer(modifier = Modifier.height(12.dp))
        DemoDataCard(username = username)
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = {
            SessionStore.clear(context)
            val intent = Intent(context, SignPage::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
            (context as Activity).finish()
        },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111111), contentColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text(text = "Log Out", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

// DEMO ONLY: adds sample workouts with simulated heart rate, so the charts,
// the plant and the leaderboard have data before the Polar H10 is connected.
@Composable
fun DemoDataCard(username: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // True while saving, so the button can't be pressed twice
    var adding by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(text = "🧪 Demo data", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Adds about 10 sample workouts from the last 2 weeks (simulated heart rate). For testing and the demo only.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    adding = true
                    scope.launch {
                        val workoutDao = AppDatabase.getDatabase(context).workoutDao()
                        // Skip demo workouts that are already saved, so pressing twice doesn't double them
                        val alreadySaved = workoutDao.getStartTimes(username)
                        val workouts = demoWorkouts(username).filter { it.startTime !in alreadySaved }

                        var uploaded = 0
                        for (workout in workouts) {
                            // Same as pressing Stop: full workout on the phone, summary online
                            workoutDao.insert(workout)
                            val summary = WorkoutSummary(
                                username = username,
                                type = workout.type,
                                startTime = workout.startTime,
                                durationSec = workout.durationSec,
                                minHr = workout.minHr,
                                avgHr = workout.avgHr,
                                maxHr = workout.maxHr,
                                points = PointsCalculator.calculate(workout.heartRateList(), PointsCalculator.DEFAULT_BASELINE_HR)
                            )
                            if (WorkoutSummaryTable.insert(summary)) {
                                uploaded++
                            }
                        }
                        adding = false
                        val message = if (workouts.isEmpty()) "Demo workouts are already added" else "Added ${workouts.size} demo workouts ($uploaded uploaded)"
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !adding,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Orange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(text = if (adding) "Adding…" else "Add demo workouts", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// Type in and save the Polar H10 device ID
@Composable
fun DeviceIdCard(username: String, savedId: String?) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    // Start with the saved ID so the user can see and edit it
    var input by remember(savedId) { mutableStateOf(savedId ?: "") }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(text = "💓 My Polar H10", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "The 8-character ID printed on the back of your H10, e.g. B5E1A12F",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = input,
                // Capital letters only, no spaces, max 8 characters
                onValueChange = { input = cleanDeviceIdInput(it) },
                placeholder = { Text("Device ID") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii
                ),
                trailingIcon = {
                    // Small green tick once the ID looks right
                    if (isValidDeviceId(input)) {
                        Text(text = "✓", color = Color(0xFF3E8E41), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (isValidDeviceId(input)) {
                        focusManager.clearFocus()
                        scope.launch {
                            // Save in the local devices table. The home card reads it with a Flow,
                            // so it shows the new ID by itself.
                            AppDatabase.getDatabase(context).deviceDao().save(Device(username, input))
                            Toast.makeText(context, "Device ID saved", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Device ID must be 8 characters (0-9, A-F)", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(text = if (savedId == null) "Save" else "Update", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
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
        // White pill: Home and History on the left, Friends and Profile on the right
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
            NavItem(
                icon = Icons.Filled.DateRange,
                label = "History",
                selected = tab == "history",
                onClick = { onTabClick("history") },
                modifier = Modifier.weight(1f)
            )
            // Empty space in the middle for the big button
            Spacer(modifier = Modifier.width(88.dp))
            NavItem(
                icon = Icons.Filled.Face,
                label = "Friends",
                selected = tab == "social",
                onClick = { onTabClick("social") },
                modifier = Modifier.weight(1f)
            )
            NavItem(
                icon = Icons.Filled.Person,
                label = "Profile",
                selected = tab == "profile",
                onClick = { onTabClick("profile") },
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
