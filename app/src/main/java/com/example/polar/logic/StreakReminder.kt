package com.example.polar.logic

import com.example.polar.data.entity.Workout
import java.util.Calendar

// The reminder is sent around this hour (7pm), early enough to still train today
const val REMINDER_HOUR = 19

// true if we should send "your streak is at risk":
// the user has a streak (streakDays > 0) but has not done any workout today yet.
// [now] is a parameter so tests can choose the time.
fun shouldRemind(workouts: List<Workout>, now: Long): Boolean {
    val todayStart = startOfDayAt(now)
    val workedOutToday = workouts.any { it.startTime >= todayStart }
    if (workedOutToday) return false
    // streakDays() counts up to yesterday when today has no workout yet (see History.kt)
    return streakDays(workouts) > 0
}

// Milliseconds from [now] until the next REMINDER_HOUR o'clock (today, or tomorrow if it has passed)
fun millisUntilNextReminder(now: Long, hour: Int = REMINDER_HOUR): Long {
    val next = Calendar.getInstance()
    next.timeInMillis = now
    next.set(Calendar.HOUR_OF_DAY, hour)
    next.set(Calendar.MINUTE, 0)
    next.set(Calendar.SECOND, 0)
    next.set(Calendar.MILLISECOND, 0)
    if (next.timeInMillis <= now) {
        next.add(Calendar.DAY_OF_MONTH, 1)
    }
    return next.timeInMillis - now
}

// Midnight at the start of the day that contains [time]
private fun startOfDayAt(time: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = time
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
