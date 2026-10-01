package com.example.polar.logic

import com.example.polar.data.entity.Workout
import com.example.polar.data.entity.heartRateList

// ---------- Plant stages ----------

data class PlantStage(val name: String, val minPoints: Int)

// The plant grows through these stages as the user earns points.
// A normal 30-45 minute workout gives about 500-800 points (see PointsCalculator),
// so the plant sprouts after the first workout and blooms after about 10 workouts.
val plantStages = listOf(
    PlantStage("Seed", 0),
    PlantStage("Sprout", 300),
    PlantStage("Seedling", 1500),
    PlantStage("Young Plant", 4000),
    PlantStage("Blooming", 7500)
)

// Index in plantStages for this many points, e.g. 2000 points -> 2 (Seedling)
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
// The rules are in PointsCalculator.kt

fun totalPoints(workouts: List<Workout>, baseline: Int): Int {
    return workouts.sumOf { PointsCalculator.calculate(it.heartRateList(), baseline) }
}

fun todayPoints(workouts: List<Workout>, baseline: Int): Int {
    val today = startOfDay(daysAgo = 0)
    return workouts
        .filter { it.startTime >= today }
        .sumOf { PointsCalculator.calculate(it.heartRateList(), baseline) }
}
