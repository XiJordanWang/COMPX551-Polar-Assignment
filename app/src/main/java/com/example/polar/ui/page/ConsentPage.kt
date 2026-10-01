package com.example.polar.ui.page

/**
 * Consent screen explaining data usage and collecting privacy mode preferences.
 */

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.polar.data.prefs.PrivacyMode
import com.example.polar.data.prefs.SettingsStore
import com.example.polar.ui.theme.Orange
import com.example.polar.ui.theme.PolarTheme
import com.example.polar.ui.theme.WorkSans
import kotlinx.coroutines.launch

const val CONSENT_VERSION = 1

class ConsentPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val username = intent.getStringExtra("username") ?: ""
        val firstName = intent.getStringExtra("firstName") ?: ""
        val lastName = intent.getStringExtra("lastName") ?: ""

        setContent {
            PolarTheme {
                ConsentScreen(firstName, username, lastName)
            }
        }
    }
}

@Composable
fun ConsentScreen(firstName: String, username: String, lastName: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedMode by remember { mutableStateOf(PrivacyMode.FULL) }

    Box(modifier = Modifier.fillMaxSize()) {
        GardenBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = "Privacy & Data Consent",
                color = Color.White,
                fontSize = 32.sp,
                fontFamily = WorkSans,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            ConsentSection(title = "What", content = "Heart rate, RR intervals, ECG, accelerometer, and assessment answers.")
            ConsentSection(title = "Why", content = "To grow your plant, show your history and personal bests.")
            ConsentSection(title = "Where", content = "On this phone (Room); summaries online only in Share mode; account online.")
            ConsentSection(title = "Who can see it", content = "Only you; in Share mode others see your first name, points and streak.")

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Choose your privacy mode:",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            PrivacyModePicker(
                selected = selectedMode,
                onSelect = { selectedMode = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        SettingsStore.setPrivacyMode(context, username, selectedMode)
                        SettingsStore.setConsent(context, username, CONSENT_VERSION)
                        Toast.makeText(context, "Consent accepted", Toast.LENGTH_SHORT).show()

                        val intent = Intent(context, MainPage::class.java).apply {
                            putExtra("firstName", firstName)
                            putExtra("lastName", lastName)
                            putExtra("username", username)
                        }
                        context.startActivity(intent)
                        (context as Activity).finish()
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = "I Agree", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ConsentSection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = content, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
    }
}

// Reusable privacy mode selection picker (Full / Share / Read-only)
@Composable
fun PrivacyModePicker(
    selected: PrivacyMode,
    onSelect: (PrivacyMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        PrivacyMode.FULL to "Full: All workout and ECG data shared publicly",
        PrivacyMode.SHARE to "Share: Share workout summaries without ECG details",
        PrivacyMode.READ_ONLY to "Read-only: Keep all workouts and health data private"
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((mode, description) in modes) {
            val isSelected = mode == selected
            val backgroundCol = if (isSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f)
            val borderCol = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(backgroundCol)
                    .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                    .clickable { onSelect(mode) }
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = mode.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Text(text = "✓", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description.substringAfter(": "),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
