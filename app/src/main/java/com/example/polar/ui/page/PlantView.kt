package com.example.polar.ui.page

/** Visual component rendering the growing plant, pot styles, plant health, and growth progress. */

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.polar.logic.PlantStyle
import com.example.polar.logic.plantStages
import com.example.polar.logic.plantStyles
import com.example.polar.logic.progressToNextStage
import com.example.polar.logic.stageIndexFor
import com.example.polar.ui.theme.Orange
import com.example.polar.ui.theme.WorkSans
import kotlin.math.cos
import kotlin.math.sin

// Pot, stem, leaf and petal colours come from the PlantStyle (logic/PlantStyles.kt).
// These two are the same for every style.
private val SoilColor = Color(0xFF6D4C41)
private val FlowerCenter = Color(0xFFFFD166)

// Big card on the home page: the plant, its stage and how many points to the next stage
@Composable
fun PlantCard(points: Int, style: PlantStyle = plantStyles.first()) {
    val stageIndex = stageIndexFor(points)
    val stage = plantStages[stageIndex]
    val progress = progressToNextStage(points)
    val isFullyGrown = stageIndex == plantStages.size - 1

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlantImage(stageIndex = stageIndex, progress = progress, modifier = Modifier.size(230.dp), style = style)

            Text(
                text = stage.name,
                color = Color.White,
                fontSize = 26.sp,
                fontFamily = WorkSans,
                fontWeight = FontWeight.Bold
            )
            Text(text = "$points points", color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp)

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                color = Orange,
                trackColor = Color.White.copy(alpha = 0.3f),
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isFullyGrown) {
                    "Fully grown! 🌸"
                } else {
                    val next = plantStages[stageIndex + 1]
                    "${next.minPoints - points} more points to ${next.name}"
                },
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp
            )
        }
    }
}

// The plant picture. Drawn with Canvas for now.
// TODO: when we have the Figma images, replace the Canvas with
// Image(painter = painterResource(plantImages[stageIndex]), ...)
@Composable
fun PlantImage(
    stageIndex: Int,
    progress: Float,
    modifier: Modifier = Modifier,
    style: PlantStyle = plantStyles.first()
) {
    // 0.0 = seed, 1.0 = fully grown. Grows a little with every point, not only at each new stage.
    val lastStage = plantStages.size - 1
    val target = ((stageIndex + progress) / lastStage).coerceIn(0f, 1f)
    val growth by animateFloatAsState(targetValue = target, animationSpec = tween(1200), label = "growth")

    // Gentle left-right sway so the plant feels alive
    val sway by rememberInfiniteTransition(label = "sway").animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "sway"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerX = w / 2

        // ----- Pot -----
        val potTop = h * 0.66f
        val potBottom = h * 0.97f
        val potTopHalf = w * 0.24f
        val potBottomHalf = w * 0.17f
        val pot = Path().apply {
            moveTo(centerX - potTopHalf, potTop)
            lineTo(centerX + potTopHalf, potTop)
            lineTo(centerX + potBottomHalf, potBottom)
            lineTo(centerX - potBottomHalf, potBottom)
            close()
        }
        drawPath(pot, Color(style.potColor))

        // Rim
        val rimHeight = h * 0.06f
        val rimHalf = potTopHalf * 1.12f
        drawRoundRect(
            color = Color(style.rimColor),
            topLeft = Offset(centerX - rimHalf, potTop - rimHeight),
            size = Size(rimHalf * 2, rimHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
        )

        // Soil
        val soilY = potTop - rimHeight
        drawOval(
            color = SoilColor,
            topLeft = Offset(centerX - rimHalf * 0.85f, soilY - h * 0.02f),
            size = Size(rimHalf * 1.7f, h * 0.04f)
        )

        // Cute face on the pot
        drawFace(centerX, potTop + (potBottom - potTop) * 0.4f, w)

        // ----- Plant (sways around the bottom of the stem) -----
        val stemBase = Offset(centerX, soilY)
        rotate(degrees = sway * growth, pivot = stemBase) {
            if (stageIndex == 0) {
                // Just a seed on the soil
                drawOval(
                    color = Color(0xFF8D6E63),
                    topLeft = Offset(centerX - w * 0.04f, soilY - h * 0.05f),
                    size = Size(w * 0.08f, h * 0.06f)
                )
            } else {
                val stemHeight = h * 0.55f * growth
                val stemTop = Offset(centerX, soilY - stemHeight)
                drawLine(Color(style.stemColor), stemBase, stemTop, strokeWidth = w * 0.025f, cap = StrokeCap.Round)

                // More pairs of leaves at higher stages (1, 2 or 3 pairs)
                val pairs = minOf(stageIndex, 3)
                val leafLength = w * 0.17f * (0.6f + 0.4f * growth)
                for (i in 0 until pairs) {
                    // Spread the pairs along the stem, from the top down
                    val along = 0.9f - i * 0.28f
                    val point = Offset(centerX, soilY - stemHeight * along)
                    drawLeaf(point, leafLength, angleDegrees = -150f, color = Color(style.leafColor))
                    drawLeaf(point, leafLength, angleDegrees = -30f, color = Color(style.leafColor))
                }

                // Flower on top when fully grown
                if (stageIndex == plantStages.size - 1) {
                    drawFlower(stemTop, w * 0.05f, Color(style.petalColor))
                }
            }
        }
    }
}

// Two dot eyes, pink cheeks and a smile
private fun DrawScope.drawFace(centerX: Float, eyeY: Float, w: Float) {
    val eyeGap = w * 0.07f
    val eyeRadius = w * 0.018f
    drawCircle(Color(0xFF3B2B2B), eyeRadius, Offset(centerX - eyeGap, eyeY))
    drawCircle(Color(0xFF3B2B2B), eyeRadius, Offset(centerX + eyeGap, eyeY))

    drawOval(
        Color(0x66FF6F91),
        topLeft = Offset(centerX - eyeGap * 1.9f, eyeY + eyeRadius),
        size = Size(w * 0.05f, w * 0.03f)
    )
    drawOval(
        Color(0x66FF6F91),
        topLeft = Offset(centerX + eyeGap * 1.9f - w * 0.05f, eyeY + eyeRadius),
        size = Size(w * 0.05f, w * 0.03f)
    )

    val smile = w * 0.06f
    drawArc(
        color = Color(0xFF3B2B2B),
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(centerX - smile / 2, eyeY - smile * 0.1f),
        size = Size(smile, smile * 0.8f),
        style = Stroke(width = w * 0.012f, cap = StrokeCap.Round)
    )
}

// A leaf shape: two curves from the base to the tip
private fun DrawScope.drawLeaf(base: Offset, length: Float, angleDegrees: Float, color: Color) {
    val angle = Math.toRadians(angleDegrees.toDouble())
    val tip = Offset(base.x + length * cos(angle).toFloat(), base.y + length * sin(angle).toFloat())
    val middle = Offset((base.x + tip.x) / 2, (base.y + tip.y) / 2)
    // Direction at 90 degrees to the leaf, to make it wide in the middle
    val width = length * 0.35f
    val side = Offset(-sin(angle).toFloat() * width, cos(angle).toFloat() * width)

    val leaf = Path().apply {
        moveTo(base.x, base.y)
        quadraticTo(middle.x + side.x, middle.y + side.y, tip.x, tip.y)
        quadraticTo(middle.x - side.x, middle.y - side.y, base.x, base.y)
        close()
    }
    drawPath(leaf, color)
}

// Six petals around a yellow centre
private fun DrawScope.drawFlower(center: Offset, radius: Float, petalColor: Color) {
    for (i in 0 until 6) {
        val angle = Math.toRadians(i * 60.0)
        val petal = Offset(
            center.x + radius * 1.1f * cos(angle).toFloat(),
            center.y + radius * 1.1f * sin(angle).toFloat()
        )
        drawCircle(petalColor, radius, petal)
    }
    drawCircle(FlowerCenter, radius * 0.8f, center)
}
