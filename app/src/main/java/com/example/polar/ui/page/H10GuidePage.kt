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

// Activity that displays a step-by-step setup guide for the Polar H10 chest strap.
class H10GuidePage : ComponentActivity() {

    // Called when the activity is first created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge layout so content extends behind status and navigation bars
        enableEdgeToEdge()

        // Set up the Jetpack Compose UI content
        setContent {
            PolarTheme {

                // Full-screen container box
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {

                    // Animated or static garden theme background view
                    GardenBackground()

                    // Main vertical layout for screen contents
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding() // Inset content below top status bar
                            .navigationBarsPadding() // Inset content above bottom navigation bar
                            .padding(20.dp) // Outer screen padding
                    ) {

                        // Screen title heading
                        Text(
                            text = "Polar H10 Guide",
                            color = Color.White,
                            fontSize = 36.sp,
                            fontFamily = WorkSans,
                            fontWeight = FontWeight.Bold
                        )

                        // Vertical spacing before instructions
                        Spacer(modifier = Modifier.height(20.dp))

                        // Step-by-step text guide for setting up and wearing the Polar H10 sensor
                        Text(
                            text =
                                """
1. Wet both electrodes.
2. Attach the sensor to the strap.
3. Wear the strap snugly around your chest.
4. Turn Bluetooth on.
5. Turn Location Services on.
6. Save your Polar device ID in Profile.
7. Wait until the device connects.
8. Set the Heart Rate Baseline
9. Click on a workout when you are ready to start.
10. Begin exercising.
                            """.trimIndent(),
                            color = Color.White,
                            fontSize = 16.sp
                        )

                        // Vertical spacing before video link
                        Spacer(modifier = Modifier.height(20.dp))

                        // Clickable text button that opens an external YouTube video guide in a browser
                        Text(
                            text = "Confused! 🎥 Check this Video Guide",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {

                                // Create intent to launch the video URL in a browser or YouTube app
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://youtu.be/XDFcxN611KY")
                                )

                                // Start the video link activity
                                this@H10GuidePage.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}
