package com.example.polar.logic

/** Helper functions for formatting time durations and timers into friendly readable strings. */

// 4800 -> "1 hr 20 min", 720 -> "12 min", 45 -> "45 s"
fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "$hours hr $minutes min"
        minutes > 0 -> "$minutes min"
        else -> "$seconds s"
    }
}

// 75 -> "01:15"
fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
