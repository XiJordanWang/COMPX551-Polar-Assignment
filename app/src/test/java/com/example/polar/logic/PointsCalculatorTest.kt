package com.example.polar.logic

import org.junit.Assert.assertEquals
import org.junit.Test

// Runs on the computer, no phone needed:
//   ./gradlew :app:testDebugUnitTest
class PointsCalculatorTest {

    private val baseline = 70

    // 10 seconds at the same heart rate
    private fun tenSecondsAt(hr: Int) = List(10) { hr }

    @Test
    fun belowPlus10_givesNoPoints() {
        assertEquals(0, PointsCalculator.calculate(tenSecondsAt(79), baseline))   // +9
    }

    @Test
    fun plus10To19_gives1PointPer10Seconds() {
        assertEquals(1, PointsCalculator.calculate(tenSecondsAt(80), baseline))   // +10
        assertEquals(1, PointsCalculator.calculate(tenSecondsAt(89), baseline))   // +19
    }

    @Test
    fun plus20To29_gives2PointsPer10Seconds() {
        assertEquals(2, PointsCalculator.calculate(tenSecondsAt(90), baseline))   // +20
        assertEquals(2, PointsCalculator.calculate(tenSecondsAt(99), baseline))   // +29
    }

    @Test
    fun plus30AndAbove_gives3PointsPer10Seconds() {
        assertEquals(3, PointsCalculator.calculate(tenSecondsAt(100), baseline))  // +30
        assertEquals(3, PointsCalculator.calculate(tenSecondsAt(180), baseline))  // +110
    }

    @Test
    fun oneMinuteHard_gives18Points() {
        // 60 seconds at +30 or more = 6 blocks of 10 seconds x 3 points
        assertEquals(18, PointsCalculator.calculate(List(60) { 150 }, baseline))
    }

    @Test
    fun mixedWorkout_addsUpEachPart() {
        // 10 s rest (0) + 10 s at +15 (1) + 10 s at +25 (2) + 10 s at +40 (3) = 6
        val heartRates = tenSecondsAt(72) + tenSecondsAt(85) + tenSecondsAt(95) + tenSecondsAt(110)
        assertEquals(6, PointsCalculator.calculate(heartRates, baseline))
    }

    @Test
    fun lessThan10Seconds_roundsDown() {
        // 5 seconds at +30 = 15 "point-seconds" -> 1 point (not 1.5)
        assertEquals(1, PointsCalculator.calculate(List(5) { 100 }, baseline))
    }

    @Test
    fun higherBaseline_givesFewerPoints() {
        // Same 120 bpm: +50 for someone resting at 70, only +15 for someone resting at 105
        assertEquals(3, PointsCalculator.calculate(tenSecondsAt(120), baseline = 70))
        assertEquals(1, PointsCalculator.calculate(tenSecondsAt(120), baseline = 105))
    }

    @Test
    fun emptyWorkout_givesNoPoints() {
        assertEquals(0, PointsCalculator.calculate(emptyList(), baseline))
    }
}
