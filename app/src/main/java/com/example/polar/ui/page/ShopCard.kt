package com.example.polar.ui.page

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.polar.logic.plantStyles
import com.example.polar.logic.unlockedStyles

// Plant shop on the Home tab. No spending: a style unlocks for good once
// total points reach its level. Locked styles are greyed out and show the points needed.
@Composable
fun ShopCard(totalPoints: Int, chosenStyleId: String, onChoose: (String) -> Unit) {
    val unlockedIds = unlockedStyles(totalPoints).map { it.id }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(text = "🛍️ Plant Shop", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Reach more points to unlock new looks for your plant.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (style in plantStyles) {
                    val unlocked = style.id in unlockedIds
                    val chosen = style.id == chosenStyleId

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            // White outline around the style in use
                            .border(
                                width = if (chosen) 2.dp else 0.dp,
                                color = if (chosen) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            // Locked styles can't be tapped and look faded
                            .clickable(enabled = unlocked && !chosen) { onChoose(style.id) }
                            .alpha(if (unlocked) 1f else 0.4f)
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // A small grown plant, so each style shows its pot, leaves and flower
                        PlantImage(stageIndex = 4, progress = 1f, modifier = Modifier.size(64.dp), style = style)
                        Text(text = style.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = when {
                                chosen -> "✓ Using"
                                unlocked -> "Tap to use"
                                else -> "🔒 %,d".format(style.unlockPoints)
                            },
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
