package com.example.polar.logic

import com.example.polar.data.entity.EcgCheck
import com.example.polar.data.entity.Workout
import com.example.polar.data.entity.heartRateList
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// Total workout minutes for each of the last [days] days, oldest first
fun minutesPerDay(workouts: List<Workout>, days: Int): List<Double> {
    val result = mutableListOf<Double>()
    for (i in days - 1 downTo 0) {
        val dayStart = startOfDay(daysAgo = i)
        val dayEnd = startOfDay(daysAgo = i - 1)
        var seconds = 0
        for (workout in workouts) {
            if (workout.startTime >= dayStart && workout.startTime < dayEnd) {
                seconds += workout.durationSec
            }
        }
        // Minutes with 1 decimal place
        result.add((seconds / 60.0 * 10).roundToInt() / 10.0)
    }
    return result
}

// Labels for the last [days] days, e.g. "Mon" (pattern "EEE") or "27" (pattern "d")
fun dayLabels(days: Int, pattern: String): List<String> {
    val format = SimpleDateFormat(pattern, Locale.ENGLISH)
    val labels = mutableListOf<String>()
    for (i in days - 1 downTo 0) {
        labels.add(format.format(Date(startOfDay(daysAgo = i))))
    }
    return labels
}

// Midnight of today minus [daysAgo] days, in milliseconds
fun startOfDay(daysAgo: Int): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    calendar.add(Calendar.DAY_OF_MONTH, -daysAgo)
    return calendar.timeInMillis
}

// Daily goal: at least this many workout minutes in one day
const val GOAL_MINUTES = 30

// How many days in a row the user has met their workout goal.
// If today's goal is not met yet, the streak still counts up to yesterday.
fun streakDays(workouts: List<Workout>): Int {
    fun isGoalMet(daysAgo: Int): Boolean {
        val start = startOfDay(daysAgo)
        val end = startOfDay(daysAgo - 1)
        val dayWorkouts = workouts.filter { it.startTime >= start && it.startTime < end }
        val minutes = dayWorkouts.sumOf { it.durationSec } / 60.0
        return minutes >= GOAL_MINUTES
    }

    var daysAgo = if (isGoalMet(0)) 0 else 1
    var streak = 0
    while (isGoalMet(daysAgo)) {
        streak++
        daysAgo++
    }
    return streak
}

// ---------- Stats Data Models & Functions ----------

data class DayStats(
    val date: String,
    val minutes: Double,
    val points: Int,
    val sessions: Int,
    val goalMet: Boolean
)

data class WeekStats(
    val weekLabel: String,
    val minutes: Double,
    val points: Int,
    val sessions: Int,
    val daysGoalMet: Int
)

data class StatDelta(
    val currentValue: Double,
    val previousValue: Double,
    val difference: Double,
    val percentChange: Double
)

data class WeekOverWeekDelta(
    val minutes: StatDelta,
    val points: StatDelta,
    val sessions: StatDelta
)

// Daily stats for the last [days] days, oldest first
fun dailyStats(workouts: List<Workout>, days: Int, maxHr: Int = 200): List<DayStats> {
    val result = mutableListOf<DayStats>()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    for (i in days - 1 downTo 0) {
        val start = startOfDay(daysAgo = i)
        val end = startOfDay(daysAgo = i - 1)
        val dayWorkouts = workouts.filter { it.startTime >= start && it.startTime < end }

        val totalSec = dayWorkouts.sumOf { it.durationSec }
        val minutes = (totalSec / 60.0 * 10).roundToInt() / 10.0
        val points = dayWorkouts.sumOf { PointsCalculator.calculate(it.heartRateList(), PointsCalculator.DEFAULT_BASELINE_HR) }
        val sessions = dayWorkouts.size
        val goalMet = minutes >= GOAL_MINUTES

        val dateStr = dateFormat.format(Date(start))
        result.add(DayStats(dateStr, minutes, points, sessions, goalMet))
    }
    return result
}

// Weekly stats for the last [weeks] weeks, oldest first
fun weeklyStats(workouts: List<Workout>, weeks: Int, maxHr: Int = 200): List<WeekStats> {
    if (weeks <= 0) return emptyList()
    val allDays = dailyStats(workouts, weeks * 7, maxHr)
    val result = mutableListOf<WeekStats>()

    val chunks = allDays.chunked(7)
    val totalChunks = chunks.size
    for ((index, chunk) in chunks.withIndex()) {
        val weeksAgo = totalChunks - 1 - index
        val minutes = (chunk.sumOf { it.minutes } * 10).roundToInt() / 10.0
        val points = chunk.sumOf { it.points }
        val sessions = chunk.sumOf { it.sessions }
        val daysGoalMet = chunk.count { it.goalMet }

        val weekLabel = if (weeksAgo == 0) "This Week" else "$weeksAgo ${if (weeksAgo == 1) "week" else "weeks"} ago"
        result.add(WeekStats(weekLabel, minutes, points, sessions, daysGoalMet))
    }
    return result
}

// Difference and percentage change between this week (last 7 days) and last week (7-14 days ago)
fun weekOverWeekDelta(workouts: List<Workout>, maxHr: Int = 200): WeekOverWeekDelta {
    val days14 = dailyStats(workouts, 14, maxHr)
    val lastWeek = days14.subList(0, 7)
    val thisWeek = days14.subList(7, 14)

    val thisMin = (thisWeek.sumOf { it.minutes } * 10).roundToInt() / 10.0
    val lastMin = (lastWeek.sumOf { it.minutes } * 10).roundToInt() / 10.0

    val thisPts = thisWeek.sumOf { it.points }.toDouble()
    val lastPts = lastWeek.sumOf { it.points }.toDouble()

    val thisSess = thisWeek.sumOf { it.sessions }.toDouble()
    val lastSess = lastWeek.sumOf { it.sessions }.toDouble()

    fun calculateDelta(current: Double, previous: Double): StatDelta {
        val diff = ((current - previous) * 10).roundToInt() / 10.0
        val pct = if (previous == 0.0) {
            if (current > 0.0) 100.0 else 0.0
        } else {
            (((current - previous) / previous) * 1000.0).roundToInt() / 10.0
        }
        return StatDelta(current, previous, diff, pct)
    }

    return WeekOverWeekDelta(
        minutes = calculateDelta(thisMin, lastMin),
        points = calculateDelta(thisPts, lastPts),
        sessions = calculateDelta(thisSess, lastSess)
    )
}

// 7-day rolling mean for daily stats (average of current day plus 6 days before)
fun rollingMean7Day(dailyStats: List<DayStats>): List<Double> {
    return dailyStats.indices.map { i ->
        val startIndex = maxOf(0, i - 6)
        val window = dailyStats.subList(startIndex, i + 1)
        val avg = window.sumOf { it.minutes } / window.size
        (avg * 10).roundToInt() / 10.0
    }
}

// ---------- Personal Bests ----------

data class LongestSessionBest(
    val durationSec: Int = 0,
    val date: String = ""
)

data class MostPointsBest(
    val points: Int = 0,
    val date: String = ""
)

data class LowestRestingHrBest(
    val restingHr: Int = 0,
    val date: String = ""
)

data class PersonalBests(
    val longestSession: LongestSessionBest = LongestSessionBest(),
    val mostPointsSession: MostPointsBest = MostPointsBest(),
    val longestStreakEver: Int = 0,
    val lowestRestingHr: LowestRestingHrBest = LowestRestingHrBest()
)

fun calculateLongestStreakEver(workouts: List<Workout>): Int {
    if (workouts.isEmpty()) return 0

    val minTime = workouts.minOf { it.startTime }

    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val todayStart = calendar.timeInMillis

    val minCalendar = Calendar.getInstance().apply {
        timeInMillis = minTime
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val minDayStart = minCalendar.timeInMillis

    val dayMillis = 24 * 60 * 60 * 1000L
    val totalDays = maxOf(1, ((todayStart - minDayStart) / dayMillis).toInt() + 1)

    var maxStreak = 0
    var currentStreak = 0

    for (i in (totalDays - 1) downTo 0) {
        val start = startOfDay(daysAgo = i)
        val end = startOfDay(daysAgo = i - 1)
        val dayWorkouts = workouts.filter { it.startTime >= start && it.startTime < end }
        val minutes = dayWorkouts.sumOf { it.durationSec } / 60.0
        if (minutes >= GOAL_MINUTES) {
            currentStreak++
            if (currentStreak > maxStreak) {
                maxStreak = currentStreak
            }
        } else {
            currentStreak = 0
        }
    }
    return maxStreak
}

fun calculatePersonalBests(
    workouts: List<Workout>,
    ecgChecks: List<EcgCheck>,
    maxHr: Int = 200
): PersonalBests {
    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)

    // Longest session (duration + date)
    val longestWorkout = workouts.maxByOrNull { it.durationSec }
    val longestSession = if (longestWorkout != null) {
        LongestSessionBest(
            durationSec = longestWorkout.durationSec,
            date = dateFormat.format(Date(longestWorkout.startTime))
        )
    } else LongestSessionBest()

    // Most points in one session (points + date)
    val mostPointsWorkout = workouts.maxByOrNull { PointsCalculator.calculate(it.heartRateList(), PointsCalculator.DEFAULT_BASELINE_HR) }
    val mostPoints = if (mostPointsWorkout != null) {
        MostPointsBest(
            points = PointsCalculator.calculate(mostPointsWorkout.heartRateList(), PointsCalculator.DEFAULT_BASELINE_HR),
            date = dateFormat.format(Date(mostPointsWorkout.startTime))
        )
    } else MostPointsBest()

    // Longest streak ever
    val streakEver = calculateLongestStreakEver(workouts)

    // Lowest resting HR (from EcgCheck)
    val lowestEcg = ecgChecks.filter { it.restingHr > 0 }.minByOrNull { it.restingHr }
    val lowestResting = if (lowestEcg != null) {
        LowestRestingHrBest(
            restingHr = lowestEcg.restingHr,
            date = dateFormat.format(Date(lowestEcg.time))
        )
    } else LowestRestingHrBest()

    return PersonalBests(
        longestSession = longestSession,
        mostPointsSession = mostPoints,
        longestStreakEver = streakEver,
        lowestRestingHr = lowestResting
    )
}
