package com.example.polar.ui.page

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.polar.ui.theme.PolarTheme
import com.example.polar.ui.theme.WorkSans
import androidx.compose.foundation.layout.Box
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable

class H10GuidePage : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            PolarTheme {

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {

                    GardenBackground()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(20.dp)
                    ) {

                        Text(
                            text = "Polar H10 Guide",
                            color = Color.White,
                            fontSize = 36.sp,
                            fontFamily = WorkSans,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text =
                                """
1. Wet both electrodes.
2. Attach the sensor to the strap.
3. Wear the strap snugly around your chest.
4. Turn Bluetooth on.
5. Turn Location Services on.
6. Save your Polar device ID in Profile.
7. Start a workout.
8. Wait until the device connects.
9. Begin exercising.
                            """.trimIndent(),
                            color = Color.White,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "🎥 Check this Video Guide",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {

                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://youtu.be/XDFcxN611KY")
                                )

                                this@H10GuidePage.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}