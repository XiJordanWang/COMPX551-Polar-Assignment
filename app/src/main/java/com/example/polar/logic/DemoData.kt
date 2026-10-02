package com.example.polar.logic

import com.example.polar.data.entity.Workout
import com.example.polar.data.model.workoutTypes
import java.util.Calendar
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// DEMO DATA ONLY — used by the "Add demo workouts" button (debug builds).
// Makes believable workouts so the charts, the plant and the leaderboard
// have something to show before the Polar H10 is connected.

// Heart rate for every second of one workout:
//   warm-up (first 10%):   rest -> target, slowly going up
//   main part (75%):       around the target, with slow waves (intervals) and small noise
//   cool-down (last 15%):  target -> a bit above rest, slowly going down
fun demoHeartRates(durationSec: Int, restHr: Int, targetHr: Int, random: Random): List<Int> {
    val warmUpEnd = (durationSec * 0.10).toInt()
    val coolDownStart = (durationSec * 0.85).toInt()
    val heartRates = mutableListOf<Int>()

    for (second in 0 until durationSec) {
        val base = when {
            second < warmUpEnd -> {
                val progress = second.toDouble() / warmUpEnd
                restHr + (targetHr - restHr) * progress
            }
            second < coolDownStart -> {
                // A wave every ~4 minutes, +-10 bpm, like harder and easier parts
                targetHr + 10 * sin(second / 40.0)
            }
            else -> {
                val progress = (second - coolDownStart).toDouble() / (durationSec - coolDownStart)
                targetHr - (targetHr - (restHr + 15)) * progress
            }
        }
        val noise = random.nextInt(-3, 4)
        heartRates.add((base + noise).roundToInt())
    }
    return heartRates
}

// Points the demo workouts add up to (what the plant will show if there are no other workouts)
const val DEMO_TARGET_POINTS = 5000

// Demo workouts worth exactly DEMO_TARGET_POINTS, one sport after another
// (Running, Walking, Swimming, ... so every sport is used), starting today and going back.
// Random(42) gives the same "random" data every time, so the demo is repeatable
// and pressing the button twice can skip what is already saved.
fun demoWorkouts(username: String): List<Workout> {
    val random = Random(42)
    val baseline = PointsCalculator.DEFAULT_BASELINE_HR
    val workouts = mutableListOf<Workout>()
    var totalPoints = 0

    for (daysAgo in 0..30) {
        if (totalPoints >= DEMO_TARGET_POINTS) break
        // Skip about 1 day in 4, like a real person (but always train today)
        if (daysAgo > 0 && random.nextInt(4) == 0) continue

        // Every sport in turn, so all of them appear in the history
        val type = workoutTypes[workouts.size % workoutTypes.size]
        val minutes = random.nextInt(20, 41)          // 20 to 40 minutes
        val targetHr = random.nextInt(115, 161)       // how hard this workout was
        val heartRates = demoHeartRates(minutes * 60, restHr = 68, targetHr = targetHr, random = random).toMutableList()

        // Last workout: cut seconds off the end until the total is exactly the target
        var points = PointsCalculator.calculate(heartRates, baseline)
        while (totalPoints + points > DEMO_TARGET_POINTS) {
            heartRates.removeAt(heartRates.size - 1)
            points = PointsCalculator.calculate(heartRates, baseline)
        }
        totalPoints += points

        // Start at 7am or 6pm on that day
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -daysAgo)
        calendar.set(Calendar.HOUR_OF_DAY, if (random.nextBoolean()) 7 else 18)
        calendar.set(Calendar.MINUTE, random.nextInt(60))
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)   // same start time every time, so we can spot duplicates
        // Today's 6pm might still be in the future, so move it back to this morning instead
        if (calendar.timeInMillis + heartRates.size * 1000L > System.currentTimeMillis()) {
            calendar.set(Calendar.HOUR_OF_DAY, 7)
            calendar.set(Calendar.MINUTE, 0)
        }

        workouts.add(
            Workout(
                username = username,
                type = type.name,
                startTime = calendar.timeInMillis,
                durationSec = heartRates.size,
                minHr = heartRates.min(),
                avgHr = heartRates.average().roundToInt(),
                maxHr = heartRates.max(),
                heartRates = heartRates.joinToString(",")
            )
        )
    }
    // Oldest first, like real workouts would be saved
    return workouts.reversed()
}
