package com.example.polar.ui.page

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.polar.data.db.AppDatabase
import com.example.polar.ui.theme.Orange
import com.example.polar.ui.theme.PolarTheme
import com.example.polar.ui.theme.WorkSans

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

    val db = remember {
        AppDatabase.getDatabase(context)
    }

    val baselineFlow = remember {
        db.baselineDao().observeBaseline(username)
    }

    val baseline by baselineFlow.collectAsState(initial = null)

    GardenBackground()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
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

        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {

                Text(
                    text = "Current Baseline",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )

                Text(
                    text = baseline?.baselineHr?.let {
                        "$it bpm"
                    } ?: "Not measured",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val intent = Intent(context, BaselinePage::class.java).apply {
                    putExtra("username", username)
                }
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Orange
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Recalculate Baseline")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                context.startActivity(
                    Intent(
                        context,
                        H10GuidePage::class.java
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF111111)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Polar H10 Guide")
        }
    }
}