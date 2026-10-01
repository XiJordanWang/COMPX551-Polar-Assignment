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
