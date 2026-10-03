package com.example.polar.logic

import com.example.polar.data.entity.Workout
import com.example.polar.data.entity.heartRateList
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt


// ---------- Plant stages ----------

data class PlantStage(
    val name: String,
    val minPoints: Int
)

val plantStages = listOf(
    PlantStage("Seed", 0),
    PlantStage("Sprout", 300),
    PlantStage("Seedling", 1500),
    PlantStage("Young Plant", 4000),
    PlantStage("Blooming", 7500)
)

fun stageIndexFor(points: Int): Int {
    var index = 0

    for (i in plantStages.indices) {
        if (points >= plantStages[i].minPoints) {
            index = i
        }
    }

    return index
}

fun progressToNextStage(points: Int): Float {
    val index = stageIndexFor(points)

    if (index == plantStages.size - 1) {
        return 1f
    }

    val current = plantStages[index].minPoints
    val next = plantStages[index + 1].minPoints

    return (
        (points - current).toFloat() /
            (next - current)
        ).coerceIn(0f, 1f)
}


// ---------- Points ----------

fun totalPoints(
    workouts: List<Workout>,
    baseline: Int,
    age: Int = 25,
    config: PointsConfig = PointsConfig()
): Int {
    return workouts.sumOf {
        PointsCalculator.calculate(
            heartRates = it.heartRateList(),
            baseline = baseline,
            age = age,
            config = config
        )
    }
}

fun todayPoints(
    workouts: List<Workout>,
    baseline: Int,
    age: Int = 25,
    config: PointsConfig = PointsConfig()
): Int {
    val today = startOfDay(daysAgo = 0)

    return workouts
        .filter { it.startTime >= today }
        .sumOf {
            PointsCalculator.calculate(
                heartRates = it.heartRateList(),
                baseline = baseline,
                age = age,
                config = config
            )
        }
}


// ---------- Daily goal ----------

fun todayActiveSeconds(
    workouts: List<Workout>,
    baseline: Int,
    age: Int = 25,
    config: PointsConfig = PointsConfig()
): Long {
    val today = startOfDay(daysAgo = 0)

    return workouts
        .filter { it.startTime >= today }
        .sumOf {
            PointsCalculator.calculateActiveSeconds(
                heartRates = it.heartRateList(),
                baseline = baseline,
                age = age,
                config = config
            )
        }
}

fun todayActiveMinutes(
    workouts: List<Workout>,
    baseline: Int,
    age: Int = 25,
    config: PointsConfig = PointsConfig()
): Double {
    return todayActiveSeconds(
        workouts = workouts,
        baseline = baseline,
        age = age,
        config = config
    ) / 60.0
}

fun validateGoalDuration(
    goalMinutes: Int,
    minimumGoalMinutes: Int = 5,
    maximumGoalMinutes: Int = 120
): Int {
    val limited =
        goalMinutes.coerceIn(
            minimumGoalMinutes,
            maximumGoalMinutes
        )

    return (
        (limited / 5.0).roundToInt() * 5
        ).coerceIn(
        minimumGoalMinutes,
        maximumGoalMinutes
    )
}

fun goalMet(
    workouts: List<Workout>,
    baseline: Int,
    age: Int,
    goalMinutes: Int,
    config: PointsConfig = PointsConfig()
): Boolean {
    val validGoal =
        validateGoalDuration(goalMinutes)

    return todayActiveMinutes(
        workouts = workouts,
        baseline = baseline,
        age = age,
        config = config
    ) >= validGoal
}

fun goalProgress(
    workouts: List<Workout>,
    baseline: Int,
    age: Int,
    goalMinutes: Int,
    config: PointsConfig = PointsConfig()
): Float {
    val validGoal =
        validateGoalDuration(goalMinutes)

    return (
        todayActiveMinutes(
            workouts = workouts,
            baseline = baseline,
            age = age,
            config = config
        ) / validGoal
        )
        .toFloat()
        .coerceIn(0f, 1f)
}

fun goalBonus(
    workouts: List<Workout>,
    baseline: Int,
    age: Int,
    goalMinutes: Int,
    bonusPointsPerMinute: Double,
    config: PointsConfig = PointsConfig()
): Int {
    val validGoal =
        validateGoalDuration(goalMinutes)

    if (
        !goalMet(
            workouts = workouts,
            baseline = baseline,
            age = age,
            goalMinutes = validGoal,
            config = config
        )
    ) {
        return 0
    }

    return (
        validGoal *
            bonusPointsPerMinute
        ).roundToInt()
}


// ---------- Streak ----------

fun calculateStreak(
    goalDates: List<LocalDate>
): Int {
    if (goalDates.isEmpty()) {
        return 0
    }

    val dates =
        goalDates
            .distinct()
            .sortedDescending()

    var streak = 1

    for (i in 0 until dates.size - 1) {
        val current =
            dates[i]

        val previous =
            dates[i + 1]

        if (
            previous ==
            current.minusDays(1)
        ) {
            streak++
        } else {
            break
        }
    }

    return streak
}

fun streakBonus(
    streak: Int,
    threeDayBonus: Int = 20,
    sevenDayBonus: Int = 50,
    thirtyDayBonus: Int = 100
): Int {
    return when (streak) {
        3 -> threeDayBonus
        7 -> sevenDayBonus
        30 -> thirtyDayBonus
        else -> 0
    }
}


// ---------- Plant health ----------

enum class PlantHealth {
    HEALTHY,
    SLIGHTLY_WILTED,
    WILTED,
    SICK,
    VERY_SICK
}

fun plantHealth(
    lastGoalMetDate: LocalDate?,
    today: LocalDate = LocalDate.now()
): PlantHealth {
    if (lastGoalMetDate == null) {
        return PlantHealth.HEALTHY
    }

    val inactiveDays =
        ChronoUnit.DAYS.between(
            lastGoalMetDate,
            today
        )

    return when {
        inactiveDays <= 0 ->
            PlantHealth.HEALTHY

        inactiveDays == 1L ->
            PlantHealth.SLIGHTLY_WILTED

        inactiveDays == 2L ->
            PlantHealth.WILTED

        inactiveDays <= 4L ->
            PlantHealth.SICK

        else ->
            PlantHealth.VERY_SICK
    }
}


// ---------- Plant status ----------

data class PlantStatus(
    val stage: PlantStage,
    val stageIndex: Int,
    val stageProgress: Float,
    val health: PlantHealth,
    val exercisePoints: Int,
    val todayPoints: Int,
    val activeMinutesToday: Double,
    val goalMinutes: Int,
    val goalProgress: Float,
    val goalMet: Boolean,
    val goalBonus: Int,
    val streak: Int,
    val streakBonus: Int,
    val totalPoints: Int
)

fun plantStatus(
    workouts: List<Workout>,
    baseline: Int,
    age: Int,
    goalMinutes: Int,
    bonusPointsPerMinute: Double,
    goalDates: List<LocalDate>,
    lastGoalMetDate: LocalDate?,
    config: PointsConfig = PointsConfig()
): PlantStatus {
    val validGoal =
        validateGoalDuration(goalMinutes)

    val exercisePoints =
        totalPoints(
            workouts = workouts,
            baseline = baseline,
            age = age,
            config = config
        )

    val currentGoalBonus =
        goalBonus(
            workouts = workouts,
            baseline = baseline,
            age = age,
            goalMinutes = validGoal,
            bonusPointsPerMinute =
                bonusPointsPerMinute,
            config = config
        )

    val currentStreak =
        calculateStreak(goalDates)

    val currentStreakBonus =
        streakBonus(currentStreak)

    val finalPoints =
        exercisePoints +
            currentGoalBonus +
            currentStreakBonus

    val currentStageIndex =
        stageIndexFor(finalPoints)

    return PlantStatus(
        stage =
            plantStages[currentStageIndex],
        stageIndex =
            currentStageIndex,
        stageProgress =
            progressToNextStage(finalPoints),
        health =
            plantHealth(lastGoalMetDate),
        exercisePoints =
            exercisePoints,
        todayPoints =
            todayPoints(
                workouts = workouts,
                baseline = baseline,
                age = age,
                config = config
            ),
        activeMinutesToday =
            todayActiveMinutes(
                workouts = workouts,
                baseline = baseline,
                age = age,
                config = config
            ),
        goalMinutes =
            validGoal,
        goalProgress =
            goalProgress(
                workouts = workouts,
                baseline = baseline,
                age = age,
                goalMinutes = validGoal,
                config = config
            ),
        goalMet =
            goalMet(
                workouts = workouts,
                baseline = baseline,
                age = age,
                goalMinutes = validGoal,
                config = config
            ),
        goalBonus =
            currentGoalBonus,
        streak =
            currentStreak,
        streakBonus =
            currentStreakBonus,
        totalPoints =
            finalPoints
    )
}