package com.example.polar.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoDataTest {

    @Test
    fun sameDemoDataEveryTime_soDuplicatesCanBeSkipped() {
        val first = demoWorkouts("test").map { it.startTime }
        val second = demoWorkouts("test").map { it.startTime }
        assertEquals(first, second)
    }

    @Test
    fun demoWorkoutsAreWorthExactlyTheTarget() {
        val total = demoWorkouts("test").sumOf {
            PointsCalculator.calculate(it.heartRates.split(",").map { hr -> hr.toInt() }, PointsCalculator.DEFAULT_BASELINE_HR)
        }
        assertEquals(DEMO_TARGET_POINTS, total)
    }

    @Test
    fun everySportIsUsed() {
        val types = demoWorkouts("test").map { it.type }.toSet()
        assertEquals(8, types.size)
    }

    @Test
    fun allDemoWorkoutsAreInThePast() {
        val now = System.currentTimeMillis()
        for (workout in demoWorkouts("test")) {
            assertTrue(workout.startTime + workout.durationSec * 1000L <= now)
        }
    }

    @Test
    fun heartRatesStayRealistic() {
        for (workout in demoWorkouts("test")) {
            assertTrue(workout.minHr >= 50)
            assertTrue(workout.maxHr <= 200)
        }
    }
}
