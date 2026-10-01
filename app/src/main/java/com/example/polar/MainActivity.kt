package com.example.polar

/**
 * Entry point activity checking user session and displaying the welcome screen.
 */

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.polar.data.prefs.SessionStore
import com.example.polar.data.prefs.SettingsStore
import com.example.polar.logic.plantStages
import com.example.polar.ui.page.CONSENT_VERSION
import com.example.polar.ui.page.ConsentPage
import com.example.polar.ui.page.GardenBackground
import com.example.polar.ui.page.MainPage
import com.example.polar.ui.page.PlantImage
import com.example.polar.ui.page.SignPage
import com.example.polar.ui.theme.Orange
import com.example.polar.ui.theme.PolarTheme
import com.example.polar.ui.theme.WorkSans
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val savedUser = SessionStore.getUser(this)
        if (savedUser != null) {
            val version = runBlocking { SettingsStore.consentVersion(this@MainActivity, savedUser.username).first() }
            val targetClass = if (version < CONSENT_VERSION) ConsentPage::class.java else MainPage::class.java
            val intent = Intent(this, targetClass).apply {
                putExtra("firstName", savedUser.firstName)
                putExtra("lastName", savedUser.lastName)
                putExtra("username", savedUser.username)
            }
            startActivity(intent)
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            PolarTheme {
                WelcomeScreen()
            }
        }
    }
}

@Composable
fun WelcomeScreen() {
    val context = LocalContext.current

    var stage by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            stage = (stage + 1) % plantStages.size
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GardenBackground(sunX = 0.5f, sunY = 0.42f, sunSize = 2.4f, cloudsY = 0.24f, hillsTop = 0.6f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Polar Garden",
                color = Color.White,
                fontSize = 44.sp,
                fontFamily = WorkSans,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Every heartbeat helps your plant grow",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 17.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            PlantImage(stageIndex = stage, progress = 0f, modifier = Modifier.size(280.dp))

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Group 6 · COMPX551",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { context.startActivity(Intent(context, SignPage::class.java)) },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Orange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = "Get Started  →", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
