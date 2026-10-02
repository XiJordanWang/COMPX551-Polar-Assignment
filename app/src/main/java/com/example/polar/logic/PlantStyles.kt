package com.example.polar.logic

// A look for the plant and its pot. Colours are 0xAARRGGBB numbers, so this file
// doesn't need Compose and can be unit tested. PlantView turns them into Color(...).
data class PlantStyle(
    val id: String,          // saved in Settings, never change it
    val name: String,        // shown to the user
    val unlockPoints: Int,   // total points needed (no spending: reaching it unlocks forever)
    val potColor: Long,
    val rimColor: Long,
    val stemColor: Long,
    val leafColor: Long,
    val petalColor: Long
)

const val DEFAULT_STYLE_ID = "classic"

val plantStyles = listOf(
    PlantStyle("classic", "Classic", 0, 0xFFE07A5F, 0xFFC8553D, 0xFF3E8E41, 0xFF6BCB77, 0xFFFF8FAB),
    PlantStyle("sakura", "Sakura", 2_000, 0xFFF4A6C1, 0xFFE07BA0, 0xFF4E9F5A, 0xFF8FD18B, 0xFFFFC2D9),
    PlantStyle("ocean", "Ocean", 5_000, 0xFF5DA9E9, 0xFF3B82C4, 0xFF2A9D8F, 0xFF4ECDC4, 0xFF9AD0F5),
    PlantStyle("golden", "Golden", 10_000, 0xFFF2C14E, 0xFFD9A21B, 0xFF3E8E41, 0xFF7BC96F, 0xFFFFE066)
)

// Styles the user can use with this many total points (Classic is always included)
fun unlockedStyles(totalPoints: Int): List<PlantStyle> {
    return plantStyles.filter { totalPoints >= it.unlockPoints }
}

// The style to draw: the saved one if it is unlocked, otherwise Classic.
// (Points can go down, for example after "Delete Data", so a saved style may be locked again.)
fun usableStyle(savedId: String?, totalPoints: Int): PlantStyle {
    val saved = unlockedStyles(totalPoints).find { it.id == savedId }
    return saved ?: plantStyles.first { it.id == DEFAULT_STYLE_ID }
}
