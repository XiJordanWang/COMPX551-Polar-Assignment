package com.example.polar.logic

// Points for one workout: heart rate + baseline in, points out.
// Pure Kotlin (no Android, no database), so it is easy to test.
//
// How far above the baseline (resting heart rate) each second is:
//   below +10 bpm   -> 0 points
//   +10 to +19 bpm  -> 1 point per 10 seconds
//   +20 to +29 bpm  -> 2 points per 10 seconds
//   +30 bpm or more -> 3 points per 10 seconds
object PointsCalculator {

    // Until the 30 second resting baseline is measured, we use a normal adult resting heart rate
    const val DEFAULT_BASELINE_HR = 70

    // heartRates: one bpm per second. baseline: the user's resting heart rate.
    fun calculate(heartRates: List<Int>, baseline: Int): Int {
        var total = 0
        for (hr in heartRates) {
            total += pointsForOneSecond(hr, baseline)
        }
        // Each second gave 0-3, and the rules are "per 10 seconds", so divide by 10
        return total / 10
    }

    // 0, 1, 2 or 3 depending on how far above the baseline this heart rate is
    fun pointsForOneSecond(hr: Int, baseline: Int): Int {
        val aboveBaseline = hr - baseline
        return when {
            aboveBaseline < 10 -> 0
            aboveBaseline < 20 -> 1
            aboveBaseline < 30 -> 2
            else -> 3
        }
    }
}
