package com.example.polar.logic

import kotlin.math.abs

// How long to wait after a coach message before the next one (5 minutes)
const val COACH_COOLDOWN_MS = 5 * 60 * 1000L

// true if the user looks inactive: every heart rate in the last [windowSec] seconds
// is within [tolerance] bpm of the resting baseline (for example 65..75 if baseline is 70).
// recentHeartRates must have one value per second, newest last.
// Fewer samples than the window -> false (we don't know yet).
fun isInactive(recentHeartRates: List<Int>, baseline: Int, windowSec: Int = 180, tolerance: Int = 5): Boolean {
    if (recentHeartRates.size < windowSec) return false
    val window = recentHeartRates.takeLast(windowSec)
    return window.all { hr -> abs(hr - baseline) <= tolerance }
}

// true if enough time has passed since the last coach message.
// lastSentAt = 0 means "never sent", so the first message can always go.
fun canSendAgain(lastSentAt: Long, now: Long, cooldownMs: Long = COACH_COOLDOWN_MS): Boolean {
    return lastSentAt == 0L || now - lastSentAt >= cooldownMs
}
