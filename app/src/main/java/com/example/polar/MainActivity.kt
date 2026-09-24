package com.example.polar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.polar.ui.theme.PolarTheme
import com.example.polar.ui.page.MainPage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PolarTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.heart_beat_is_shown_black_background),
                        contentDescription = "Background",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent
                    ) { innerPadding ->
                        Greeting(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(modifier: Modifier = Modifier) {
    val rainbowColors = listOf(Color.Yellow, Color.Cyan, Color.Magenta)
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, bottom = 128.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // https://developer.android.com/develop/ui/compose/text/style-text
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            brush = Brush.linearGradient(colors = rainbowColors),
                            fontStyle = FontStyle.Italic
                        )
                    ) { append("Group 6's") }
                    append(" assignment!")
                },
                color = Color.White,
                fontSize = 32.sp
            )

            // https://developer.android.com/develop/ui/compose/components/button
            ElevatedButton(
                onClick = { context.startActivity(Intent(context, MainPage::class.java)) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5722),
                    contentColor = Color.White
                ),
                modifier = Modifier.size(width = 207.dp, height = 64.dp)
            ) {
                Text(text = "Watch Data", fontSize = 16.sp)


            }
        }
    }
}