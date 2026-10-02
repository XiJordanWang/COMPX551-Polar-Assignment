package com.example.polar.logic

import com.example.polar.data.entity.Workout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class StreakReminderTest {

    private val now = System.currentTimeMillis()

    // A 40-minute workout (meets the 30-minute daily goal) that started [daysAgo] days ago at 8am
    private fun workout(daysAgo: Int, minutes: Int = 40): Workout {
        val c = Calendar.getInstance()
        c.timeInMillis = now
        c.add(Calendar.DAY_OF_MONTH, -daysAgo)
        c.set(Calendar.HOUR_OF_DAY, 8)
        c.set(Calendar.MINUTE, 0)
        return Workout(
            username = "test", type = "Running", startTime = c.timeInMillis,
            durationSec = minutes * 60, minHr = 80, avgHr = 130, maxHr = 160, heartRates = ""
        )
    }

    @Test
    fun streakAndNoWorkoutToday_reminds() {
        val workouts = listOf(workout(daysAgo = 1), workout(daysAgo = 2))
        assertTrue(shouldRemind(workouts, now))
    }

    @Test
    fun workoutToday_noReminder() {
        val workouts = listOf(workout(daysAgo = 0), workout(daysAgo = 1))
        assertFalse(shouldRemind(workouts, now))
    }

    @Test
    fun noStreak_noReminder() {
        // Last workout 3 days ago: the streak is already 0, nothing to save
        assertFalse(shouldRemind(listOf(workout(daysAgo = 3)), now))
        assertFalse(shouldRemind(emptyList(), now))
    }

    @Test
    fun nextReminder_laterToday_orTomorrow() {
        // A fixed day with no daylight-saving change, so every day is exactly 24 hours
        val c = Calendar.getInstance()
        c.set(2026, Calendar.JULY, 15, 10, 0, 0)
        c.set(Calendar.MILLISECOND, 0)
        val tenAm = c.timeInMillis
        assertEquals(9 * 60 * 60 * 1000L, millisUntilNextReminder(tenAm, hour = 19))   // 10am -> 7pm today

        c.set(Calendar.HOUR_OF_DAY, 20)
        val eightPm = c.timeInMillis
        assertEquals(23 * 60 * 60 * 1000L, millisUntilNextReminder(eightPm, hour = 19)) // 8pm -> 7pm tomorrow
    }
}
