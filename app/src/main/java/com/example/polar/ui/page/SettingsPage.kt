package com.example.polar.ui.page

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.polar.data.DataGate
import com.example.polar.data.db.AppDatabase
import com.example.polar.data.entity.heartRateList
import com.example.polar.data.online.AssessmentTable
import com.example.polar.data.online.UserTable
import com.example.polar.data.online.WorkoutSummary
import com.example.polar.data.online.WorkoutSummaryTable
import com.example.polar.data.prefs.PrivacyMode
import com.example.polar.data.prefs.SessionStore
import com.example.polar.data.prefs.SettingsStore
import com.example.polar.logic.CoachMode
import com.example.polar.logic.PointsCalculator
import com.example.polar.logic.streakDays
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

class SettingsPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val username = intent.getStringExtra("username") ?: ""

        setContent {
            PolarTheme {
                SettingsScreen(username)
            }
        }
    }
}

@Composable
fun SettingsScreen(username: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    // Privacy Mode
    val modeFlow = remember { SettingsStore.privacyMode(context, username) }
    val currentMode by modeFlow.collectAsState(initial = PrivacyMode.FULL)

    // Device ID
    val deviceIdFlow = remember { db.deviceDao().observeDeviceId(username) }
    val deviceId by deviceIdFlow.collectAsState(initial = null)

    // Baseline
    val baselineFlow = remember { db.baselineDao().observeBaseline(username) }
    val baseline by baselineFlow.collectAsState(initial = null)

    // Coach Personality
    val coachModeFlow = remember { SettingsStore.coachMode(context, username) }
    val coachMode by coachModeFlow.collectAsState(initial = CoachMode.SUPPORTIVE)

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteAccountToo by remember { mutableStateOf(false) }

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

    GardenBackground()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Settings",
            color = Color.White,
            fontSize = 36.sp,
            fontFamily = WorkSans,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Privacy & Cloud Sharing Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(text = "🔒 Privacy & Data Sharing", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                PrivacyModePicker(
                    selected = currentMode,
                    onSelect = { newMode ->
                        scope.launch {
                            SettingsStore.setPrivacyMode(context, username, newMode)
                            val isSharing = newMode == PrivacyMode.SHARE
                            UserTable.setSharing(username, isSharing)

                            if (!isSharing) {
                                WorkoutSummaryTable.deleteForUser(username)
                                AssessmentTable.deleteForUser(username)
                                val onlineAssessment = AssessmentTable.findByUsername(username)
                                if (onlineAssessment != null) {
                                    SettingsStore.setAssessment(context, username, onlineAssessment)
                                }
                                Toast.makeText(context, "Privacy mode updated. Saved locally.", Toast.LENGTH_SHORT).show()
                            } else {
                                WorkoutSummaryTable.deleteForUser(username)
                                val localWorkouts = db.workoutDao().getWorkouts(username).first()
                                val baselineEntity = db.baselineDao().observeBaseline(username).first()
                                val baseline = baselineEntity?.baselineHr ?: PointsCalculator.DEFAULT_BASELINE_HR
                                val assessment = DataGate.loadAssessment(context, username)
                                val age = assessment?.age ?: 25

                                var uploaded = 0
                                for (w in localWorkouts) {
                                    val points = PointsCalculator.calculate(w.heartRateList(), baseline, age)
                                    val summary = WorkoutSummary(
                                        username = username,
                                        type = w.type,
                                        startTime = w.startTime,
                                        durationSec = w.durationSec,
                                        minHr = w.minHr,
                                        avgHr = w.avgHr,
                                        maxHr = w.maxHr,
                                        points = points
                                    )
                                    if (WorkoutSummaryTable.insert(summary)) uploaded++
                                }
                                val streak = streakDays(localWorkouts)
                                UserTable.setStreak(username, streak)
                                val localAssessment = SettingsStore.getAssessment(context, username)
                                if (localAssessment != null) {
                                    AssessmentTable.save(localAssessment)
                                }
                                Toast.makeText(context, "Cloud sharing enabled. $uploaded past workouts uploaded.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Sensor & Device Card
        DeviceIdCard(username = username, savedId = deviceId)

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                context.startActivity(Intent(context, H10GuidePage::class.java))
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111111)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Polar H10 Setup Guide")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Health Calibration (Baseline)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(text = "Current Baseline", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Text(
                    text = baseline?.baselineHr?.let { "$it bpm" } ?: "Not measured",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val intent = Intent(context, BaselinePage::class.java).apply {
                    putExtra("username", username)
                }
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Orange),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Recalculate Baseline")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Coach Personality Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(text = "Coach personality", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Text(
                    text = when (coachMode) {
                        CoachMode.SUPPORTIVE -> "Kind and encouraging messages"
                        CoachMode.BULLY -> "Playful teasing about your plant"
                        CoachMode.MIXED -> "A bit of both"
                        CoachMode.OFF -> "No coach messages"
                    },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                ChoiceRow(
                    options = CoachMode.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    selected = coachMode.name.lowercase().replaceFirstChar { c -> c.uppercase() },
                    onSelect = { label ->
                        scope.launch {
                            SettingsStore.setCoachMode(context, username, CoachMode.valueOf(label.uppercase()))
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Manage Data Card (Export CSV & Delete Data)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(text = "📁 Manage Data", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Export your workout sessions and ECG checks as a CSV file, or delete your saved data.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
                            exportLauncher.launch("polar_data_$timestamp.csv")
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(text = "Export (CSV)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { showDeleteDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(text = "Delete Data", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        Dialog(onDismissRequest = { showDeleteDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                Text(
                    text = "Delete My Data",
                    color = Color.Black,
                    fontSize = 22.sp,
                    fontFamily = WorkSans,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This can't be undone. Are you sure you want to delete all workouts and health data? Export first if you want to keep a copy.",
                    color = Color.DarkGray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = deleteAccountToo,
                        onCheckedChange = { deleteAccountToo = it },
                        colors = CheckboxDefaults.colors(checkedColor = Orange)
                    )
                    Text(
                        text = "Delete account too",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            scope.launch {
                                // 1. Room DAOs
                                db.workoutDao().deleteForUser(username)
                                db.ecgDao().deleteForUser(username)
                                db.deviceDao().deleteForUser(username)
                                db.baselineDao().deleteForUser(username)

                                // 2. Online tables
                                WorkoutSummaryTable.deleteForUser(username)
                                AssessmentTable.deleteForUser(username)

                                if (deleteAccountToo) {
                                    UserTable.deleteUser(username)
                                }

                                // 3. DataStore keys
                                SettingsStore.clearUserData(context, username)

                                // 4. Clear session and log out
                                SessionStore.clear(context)
                                Toast.makeText(context, "Data deleted", Toast.LENGTH_SHORT).show()

                                val intent = Intent(context, SignPage::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                context.startActivity(intent)
                                (context as Activity).finish()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Confirm Delete", color = Color.White)
                    }
                }
            }
        }
    }
}
