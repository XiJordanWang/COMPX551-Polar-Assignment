package com.example.polar.ui.page

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.polar.data.DataGate
import com.example.polar.data.SaveResult
import com.example.polar.data.online.Assessment
import com.example.polar.data.online.AssessmentTable
import com.example.polar.logic.bmiCategory
import com.example.polar.logic.calculateBmi
import com.example.polar.logic.maxHeartRate
import com.example.polar.logic.zoneLimits
import com.example.polar.ui.theme.FieldGrey
import com.example.polar.ui.theme.Orange
import com.example.polar.ui.theme.PolarTheme
import com.example.polar.ui.theme.WorkSans
import kotlinx.coroutines.launch

class AssessmentPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val username = intent.getStringExtra("username") ?: ""

        setContent {
            PolarTheme {
                AssessmentScreen(username)
            }
        }
    }
}

@Composable
fun AssessmentScreen(username: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var gender by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var workoutsPerWeek by remember { mutableStateOf("") }
    var intensity by remember { mutableStateOf("") }
    // The last saved assessment, used to show the results
    var saved by remember { mutableStateOf<Assessment?>(null) }

    // If the user did the assessment before, fill in the form
    LaunchedEffect(Unit) {
        // Load assessment (local or online based on privacy mode)
        val old = DataGate.loadAssessment(context, username)
        if (old != null) {
            gender = old.gender
            age = old.age.toString()
            height = old.heightCm.toString()
            weight = old.weightKg.toString()
            workoutsPerWeek = old.workoutsPerWeek
            intensity = old.intensity
            saved = old
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Assessment",
            color = Color.Black,
            fontSize = 36.sp,
            fontFamily = WorkSans,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Tell us about yourself so we can set your heart rate zones.",
            color = Color.DarkGray,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        FieldLabel("Gender")
        ChoiceRow(options = listOf("Male", "Female"), selected = gender, onSelect = { gender = it })

        Spacer(modifier = Modifier.height(20.dp))

        FieldLabel("Age")
        NumberField(value = age, onValueChange = { age = it }, placeholder = "e.g. 25")

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                FieldLabel("Height (cm)")
                NumberField(value = height, onValueChange = { height = it }, placeholder = "e.g. 175")
            }
            Column(modifier = Modifier.weight(1f)) {
                FieldLabel("Weight (kg)")
                NumberField(value = weight, onValueChange = { weight = it }, placeholder = "e.g. 70", decimal = true)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        FieldLabel("Workouts per week")
        ChoiceRow(
            options = listOf("0", "1-2", "3-4", "5+"),
            selected = workoutsPerWeek,
            onSelect = { workoutsPerWeek = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        FieldLabel("Preferred intensity")
        ChoiceRow(
            options = listOf("Light", "Moderate", "Hard"),
            selected = intensity,
            onSelect = { intensity = it }
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                // Hide the keyboard
                focusManager.clearFocus()

                // toIntOrNull() gives null if the text is not a number
                val ageNumber = age.toIntOrNull()
                val heightNumber = height.toIntOrNull()
                val weightNumber = weight.toDoubleOrNull()

                if (gender == "" || workoutsPerWeek == "" || intensity == "") {
                    Toast.makeText(context, "Please answer every question", Toast.LENGTH_SHORT).show()
                } else if (ageNumber == null || ageNumber < 10 || ageNumber > 100) {
                    Toast.makeText(context, "Age must be between 10 and 100", Toast.LENGTH_SHORT).show()
                } else if (heightNumber == null || heightNumber < 100 || heightNumber > 250) {
                    Toast.makeText(context, "Height must be between 100 and 250 cm", Toast.LENGTH_SHORT).show()
                } else if (weightNumber == null || weightNumber < 30 || weightNumber > 300) {
                    Toast.makeText(context, "Weight must be between 30 and 300 kg", Toast.LENGTH_SHORT).show()
                } else {
                    val assessment = Assessment(
                        username = username,
                        gender = gender,
                        age = ageNumber,
                        heightCm = heightNumber,
                        weightKg = weightNumber,
                        workoutsPerWeek = workoutsPerWeek,
                        intensity = intensity
                    )
                    scope.launch {
                        val result = DataGate.saveAssessment(context, username, assessment)
                        saved = assessment
                        val message = when (result) {
                            SaveResult.SAVED_LOCAL_AND_ONLINE -> "Assessment saved & uploaded to cloud"
                            SaveResult.SAVED_LOCAL_ONLY -> "Assessment saved on this phone"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111111), contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(text = "Save", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        // Results, only shown after saving
        val result = saved
        if (result != null) {
            Spacer(modifier = Modifier.height(28.dp))
            AssessmentResult(result)
        }
    }
}

@Composable
fun AssessmentResult(assessment: Assessment) {
    val bmi = calculateBmi(assessment.heightCm, assessment.weightKg)
    val maxHr = maxHeartRate(assessment.age)
    val limits = zoneLimits(maxHr)

    Text(text = "Your Results", color = Color.Black, fontSize = 24.sp, fontFamily = WorkSans, fontWeight = FontWeight.Bold)

    Spacer(modifier = Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ResultBox(label = "BMI", value = String.format("%.1f", bmi), note = bmiCategory(bmi), modifier = Modifier.weight(1f))
        ResultBox(label = "Max Heart Rate", value = "$maxHr", note = "220 - age", modifier = Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(20.dp))

    FieldLabel("Your heart rate zones")
    ZoneRow(color = Color(0xFF7FB3D5), name = "Rest", range = "under ${limits[0]} bpm")
    ZoneRow(color = Color(0xFF58D68D), name = "Light", range = "${limits[0]} - ${limits[1]} bpm")
    ZoneRow(color = Color(0xFFF4D03F), name = "Moderate", range = "${limits[1]} - ${limits[2]} bpm")
    ZoneRow(color = Color(0xFFFF6B1A), name = "Hard", range = "${limits[2]} - ${limits[3]} bpm")
    ZoneRow(color = Color(0xFFE74C3C), name = "Maximum", range = "over ${limits[3]} bpm")

    Spacer(modifier = Modifier.height(20.dp))

    // Target range for the intensity the user likes
    val target = when (assessment.intensity) {
        "Light" -> "${limits[0]} - ${limits[1]}"
        "Moderate" -> "${limits[1]} - ${limits[2]}"
        else -> "${limits[2]} - ${limits[3]}"
    }
    val frequencyTip = if (assessment.workoutsPerWeek == "0" || assessment.workoutsPerWeek == "1-2") {
        "Try to work out at least 3 times a week."
    } else {
        "Great, keep up your routine!"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Orange.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(text = "Recommendation", color = Orange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "You like ${assessment.intensity.lowercase()} workouts, so aim for $target bpm. $frequencyTip",
            color = Color.Black,
            fontSize = 15.sp
        )
    }
}

// A row of buttons where only one can be picked
@Composable
fun ChoiceRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (option in options) {
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(if (isSelected) Orange else FieldGrey, RoundedCornerShape(16.dp))
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    color = if (isSelected) Color.White else Color.Black,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun NumberField(value: String, onValueChange: (String) -> Unit, placeholder: String, decimal: Boolean = false) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        shape = RoundedCornerShape(16.dp),
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ResultBox(label: String, value: String, note: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(FieldGrey, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(text = label, color = Color.DarkGray, fontSize = 14.sp)
        Text(text = value, color = Color.Black, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(text = note, color = Orange, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ZoneRow(color: Color, name: String, range: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = name, color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(text = range, color = Color.DarkGray, fontSize = 15.sp)
    }
}
