package com.example.polar.ui.page

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.polar.data.online.User
import com.example.polar.data.online.UserTable
import com.example.polar.data.prefs.SessionStore
import com.example.polar.data.prefs.SettingsStore
import com.example.polar.logic.checkPassword
import kotlinx.coroutines.flow.first
import com.example.polar.logic.hashPassword
import com.example.polar.ui.theme.FieldGrey
import com.example.polar.ui.theme.Orange
import com.example.polar.ui.theme.PolarTheme
import com.example.polar.ui.theme.WorkSans
import kotlinx.coroutines.launch

class SignPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PolarTheme {
                SignScreen()
            }
        }
    }
}

@Composable
fun SignScreen() {
    val context = LocalContext.current
    // The online table functions are suspend functions (they wait for the network),
    // so they need a coroutine
    val scope = rememberCoroutineScope()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    // true = sign in, false = sign up
    var isSignIn by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Garden on top, sun in the corner, hills just above the white form
        GardenBackground(sunX = 0.86f, sunY = 0.05f, sunSize = 1.3f, showClouds = false, hillsTop = 0.24f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
        ) {
            // Title and a small plant pot: a seed when signing up, a sprout when signing in
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isSignIn) "Sign In To Polar" else "Sign Up To Polar",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontFamily = WorkSans,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isSignIn) "Welcome back, your plant missed you" else "Plant your first seed with us",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp,
                    fontFamily = WorkSans
                )
                PlantImage(
                    stageIndex = if (isSignIn) 1 else 0,
                    progress = 0.5f,
                    modifier = Modifier.size(130.dp)
                )
            }

            // White panel with the form. It scrolls by itself when the keyboard is open.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // First and last name are only needed when signing up
                if (!isSignIn) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("First Name")
                            TextField(
                                value = firstName,
                                onValueChange = { firstName = it },
                                placeholder = { Text("First name") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = fieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("Last Name")
                            TextField(
                                value = lastName,
                                onValueChange = { lastName = it },
                                placeholder = { Text("Last name") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = fieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Username
                FieldLabel("Username")
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("Enter your username") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Password
                FieldLabel("Password")
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Enter your password") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Text(
                            text = if (showPassword) "Hide" else "Show",
                            color = Color.Gray,
                            modifier = Modifier
                                .clickable { showPassword = !showPassword }
                                .padding(12.dp)
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (!isSignIn && (firstName.isBlank() || lastName.isBlank())) {
                            Toast.makeText(context, "Please enter your first and last name", Toast.LENGTH_SHORT).show()
                        } else if (username.isBlank()) {
                            Toast.makeText(context, "Please enter a username", Toast.LENGTH_SHORT).show()
                        } else if (password.length < 6) {
                            Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                        } else {
                            scope.launch {
                                val user = UserTable.findByUsername(username)
                                if (isSignIn) {
                                    // Compare with the saved hash, we never store the real password
                                    if (user != null && checkPassword(password, user.passwordHash)) {
                                        SessionStore.saveUser(context, user.username, user.firstName, user.lastName)
                                        Toast.makeText(context, "Welcome back, ${user.firstName}!", Toast.LENGTH_SHORT).show()
                                        val version = SettingsStore.consentVersion(context, user.username).first()
                                        val targetClass = if (version < CONSENT_VERSION) ConsentPage::class.java else MainPage::class.java
                                        val intent = Intent(context, targetClass).apply {
                                            putExtra("firstName", user.firstName)
                                            putExtra("lastName", user.lastName)
                                            putExtra("username", user.username)
                                        }
                                        context.startActivity(intent)
                                        (context as Activity).finish()
                                    } else {
                                        Toast.makeText(context, "Wrong username or password (or no internet)", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    if (user != null) {
                                        Toast.makeText(context, "This username is already taken", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val saved = UserTable.insert(
                                            User(
                                                username = username,
                                                firstName = firstName,
                                                lastName = lastName,
                                                passwordHash = hashPassword(password)
                                            )
                                        )
                                        if (saved) {
                                            SessionStore.saveUser(context, username, firstName, lastName)
                                            Toast.makeText(context, "Welcome to Polar, $firstName!", Toast.LENGTH_SHORT).show()
                                            val version = SettingsStore.consentVersion(context, username).first()
                                            val targetClass = if (version < CONSENT_VERSION) ConsentPage::class.java else MainPage::class.java
                                            val intent = Intent(context, targetClass).apply {
                                                putExtra("firstName", firstName)
                                                putExtra("lastName", lastName)
                                                putExtra("username", username)
                                            }
                                            context.startActivity(intent)
                                            (context as Activity).finish()
                                        } else {
                                            Toast.makeText(context, "Could not sign up, check your internet", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111111),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(text = if (isSignIn) "Sign In  →" else "Sign Up  →", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Switch between sign in and sign up
                Row(horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = if (isSignIn) "Don't have an account? " else "Already have an account? ",
                        color = Color.Gray
                    )
                    Text(
                        text = if (isSignIn) "Sign Up." else "Sign In.",
                        color = Orange,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            isSignIn = !isSignIn
                            password = ""
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(
        text = text,
        color = Color.Black,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

// Grey box with no underline, like the design
@Composable
fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = FieldGrey,
    unfocusedContainerColor = FieldGrey,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    cursorColor = Orange
)
