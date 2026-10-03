package com.example.polar.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.polar.data.prefs.PrivacyMode
import com.example.polar.data.prefs.description
import com.example.polar.data.prefs.title

// Reusable privacy mode selection picker (Full / Share / Read-only)
@Composable
fun PrivacyModePicker(
    selected: PrivacyMode,
    onSelect: (PrivacyMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = PrivacyMode.entries

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (mode in modes) {
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
                            text = mode.title,
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
                        text = mode.description,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
