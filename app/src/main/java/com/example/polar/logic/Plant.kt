package com.example.polar.logic

import com.example.polar.data.entity.Workout
import com.example.polar.data.entity.heartRateList

// ---------- Plant stages ----------

data class PlantStage(val name: String, val minPoints: Int)

// The plant grows through these stages as the user earns points
val plantStages = listOf(
    PlantStage("Seed", 0),
    PlantStage("Sprout", 20),
    PlantStage("Seedling", 60),
    PlantStage("Young Plant", 150),
    PlantStage("Blooming", 300)
)

// Index in plantStages for this many points, e.g. 75 points -> 2 (Seedling)
fun stageIndexFor(points: Int): Int {
    var index = 0
    for (i in plantStages.indices) {
        if (points >= plantStages[i].minPoints) {
            index = i
        }
    }
    return index
}

// How far we are to the next stage, 0.0 to 1.0 (1.0 when fully grown)
fun progressToNextStage(points: Int): Float {
    val index = stageIndexFor(points)
    if (index == plantStages.size - 1) return 1f
    val current = plantStages[index].minPoints
    val next = plantStages[index + 1].minPoints
    return (points - current).toFloat() / (next - current)
}

// ---------- Points ----------
// TODO: this is a placeholder. Replace with the team's baseline algorithm
// (heart rate reserve from the 30 s resting baseline) when it is ready.

const val GOAL_MINUTES = 30
const val GOAL_BONUS = 20

// Points per minute in each zone: Rest 0, Light 1, Moderate 2, Hard / Maximum 3.
// Finishing a 30 minute workout gives a bonus.
fun workoutPoints(heartRates: List<Int>, maxHr: Int): Int {
    val limits = zoneLimits(maxHr)
    var pointSeconds = 0
    for (hr in heartRates) {
        pointSeconds += when {
            hr < limits[0] -> 0
            hr < limits[1] -> 1
            hr < limits[2] -> 2
            else -> 3
        }
    }
    // One heart rate per second, so divide by 60 to get "per minute"
    var points = pointSeconds / 60
    if (heartRates.size >= GOAL_MINUTES * 60) {
        points += GOAL_BONUS
    }
    return points
}

fun totalPoints(workouts: List<Workout>, maxHr: Int): Int {
    return workouts.sumOf { workoutPoints(it.heartRateList(), maxHr) }
}

fun todayPoints(workouts: List<Workout>, maxHr: Int): Int {
    val today = startOfDay(daysAgo = 0)
    return workouts
        .filter { it.startTime >= today }
        .sumOf { workoutPoints(it.heartRateList(), maxHr) }
}
