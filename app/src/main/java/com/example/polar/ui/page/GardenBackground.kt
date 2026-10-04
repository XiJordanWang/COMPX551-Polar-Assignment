package com.example.polar.ui.page

/** Animated background drawing gentle nature elements for the garden view. */

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

private val SkyTop = Color(0xFFF7743B)
private val SkyMiddle = Color(0xFFFB8D4B)
private val SkyBottom = Color(0xFFFFA862)
private val Sun = Color(0xFFFFE08A)
private val HillBack = Color(0xFFA5D46A)
private val HillMiddle = Color(0xFF8CC456)
private val HillFront = Color(0xFF6FAE45)

// Cute garden: orange sky, sun, drifting clouds and green hills.
// Drawn with Canvas so it needs no image and looks sharp on every screen size.
// sunX / sunY / sunSize, cloudsY and hillsTop let each page use a different layout,
// all as a fraction of the screen (0.0 = left/top, 1.0 = right/bottom).
// showClouds = false for pages with no empty space for clouds to float in.
@Composable
fun GardenBackground(
    modifier: Modifier = Modifier,
    sunX: Float = 0.64f,
    sunY: Float = 0.035f,
    sunSize: Float = 1f,
    showClouds: Boolean = true,
    cloudsY: Float = 0.05f,
    hillsTop: Float = 0.74f
) {
    // 0.0 -> 1.0 over 40 seconds, then starts again. Moves the clouds.
    val drift by rememberInfiniteTransition(label = "clouds").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(40000, easing = LinearEasing)),
        label = "drift"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Sky
        drawRect(Brush.verticalGradient(listOf(SkyTop, SkyMiddle, SkyBottom)))

        // Sun with a soft glow, in the gap between the title and the avatar
        val sunCenter = Offset(w * sunX, h * sunY)
        drawCircle(Sun.copy(alpha = 0.2f), radius = w * 0.16f * sunSize, center = sunCenter)
        drawCircle(Sun.copy(alpha = 0.4f), radius = w * 0.11f * sunSize, center = sunCenter)
        drawCircle(Sun, radius = w * 0.07f * sunSize, center = sunCenter)

        // Clouds stay near the top so they don't cover text in the cards.
        // They move slowly to the right and come back on the left.
        if (showClouds) {
            drawCloud(Offset(cloudX(0.05f, drift, w), h * cloudsY), w * 0.04f)
            drawCloud(Offset(cloudX(0.6f, drift * 0.7f, w), h * (cloudsY + 0.09f)), w * 0.035f)
        }

        // Hills, back to front
        drawOval(HillBack, topLeft = Offset(-w * 0.35f, h * hillsTop), size = Size(w * 1.1f, h * 0.5f))
        drawOval(HillMiddle, topLeft = Offset(w * 0.3f, h * (hillsTop + 0.04f)), size = Size(w * 1.1f, h * 0.5f))
        drawOval(HillFront, topLeft = Offset(-w * 0.15f, h * (hillsTop + 0.12f)), size = Size(w * 1.3f, h * 0.4f))
    }
}

// Where a cloud is across the screen. Wraps around so it never disappears for long.
private fun cloudX(start: Float, drift: Float, width: Float): Float {
    val position = (start + drift) % 1.2f
    return (position - 0.1f) * width
}

// A fluffy cloud made of four circles
private fun DrawScope.drawCloud(center: Offset, r: Float) {
    val cloud = Color.White.copy(alpha = 0.35f)
    drawCircle(cloud, r, Offset(center.x - r * 1.2f, center.y + r * 0.3f))
    drawCircle(cloud, r * 1.4f, center)
    drawCircle(cloud, r * 1.1f, Offset(center.x + r * 1.3f, center.y + r * 0.2f))
    drawCircle(cloud, r * 0.9f, Offset(center.x + r * 0.4f, center.y + r * 0.6f))
}
