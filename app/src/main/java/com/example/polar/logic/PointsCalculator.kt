package com.example.polar.logic

import kotlin.math.abs
import kotlin.math.roundToInt


// ---------- Configuration ----------

data class PointsConfig(
    val basePointsPerMinute: Double = 10.0,
    val lowIntensityMultiplier: Double = 1.0,
    val moderateIntensityMultiplier: Double = 1.5,
    val intenseIntensityMultiplier: Double = 2.0,
    val upperHeartRateReserveFraction: Double = 0.85,
    val minimumValidHeartRate: Int = 35,
    val maximumSuddenChangeBpm: Int = 40,
    val staleReadingSeconds: Long = 5
)


// ---------- Exercise zones ----------

enum class ExerciseZone {
    BELOW_TARGET,
    LOW,
    MODERATE,
    INTENSE,
    ABOVE_CAP
}


// ---------- Heart rate warnings ----------

enum class HeartRateWarning {
    NONE,
    INVALID_READING,
    UNUSUALLY_LOW,
    ABOVE_ESTIMATED_MAX,
    SUDDEN_CHANGE,
    STALE_READING
}

data class HeartRateCheck(
    val isValid: Boolean,
    val warning: HeartRateWarning,
    val message: String? = null
)


// ---------- Points calculator ----------

object PointsCalculator {

    const val DEFAULT_BASELINE_HR = 70

    fun calculate(
        heartRates: List<Int>,
        baseline: Int,
        age: Int,
        config: PointsConfig = PointsConfig()
    ): Int {

        if (heartRates.isEmpty()) {
            return 0
        }

        var lowSeconds = 0
        var moderateSeconds = 0
        var intenseSeconds = 0

        for (hr in heartRates) {

            val check = checkHeartRateReading(
                heartRate = hr,
                previousHeartRate = null,
                age = age,
                config = config
            )

            if (!check.isValid) {
                continue
            }

            when (
                getZone(
                    hr = hr,
                    baseline = baseline,
                    age = age,
                    config = config
                )
            ) {
                ExerciseZone.LOW ->
                    lowSeconds++

                ExerciseZone.MODERATE ->
                    moderateSeconds++

                ExerciseZone.INTENSE ->
                    intenseSeconds++

                else ->
                    Unit
            }
        }

        val lowPoints =
            calculateZonePoints(
                seconds = lowSeconds,
                multiplier = config.lowIntensityMultiplier,
                config = config
            )

        val moderatePoints =
            calculateZonePoints(
                seconds = moderateSeconds,
                multiplier = config.moderateIntensityMultiplier,
                config = config
            )

        val intensePoints =
            calculateZonePoints(
                seconds = intenseSeconds,
                multiplier = config.intenseIntensityMultiplier,
                config = config
            )

        return (
            lowPoints +
                moderatePoints +
                intensePoints
            ).roundToInt()
    }

    fun calculateActiveSeconds(
        heartRates: List<Int>,
        baseline: Int,
        age: Int,
        config: PointsConfig = PointsConfig()
    ): Long {

        if (heartRates.isEmpty()) {
            return 0
        }

        return heartRates.count { hr ->

            val check = checkHeartRateReading(
                heartRate = hr,
                previousHeartRate = null,
                age = age,
                config = config
            )

            if (!check.isValid) {
                false
            } else {
                when (
                    getZone(
                        hr = hr,
                        baseline = baseline,
                        age = age,
                        config = config
                    )
                ) {
                    ExerciseZone.LOW,
                    ExerciseZone.MODERATE,
                    ExerciseZone.INTENSE ->
                        true

                    else ->
                        false
                }
            }
        }.toLong()
    }

    fun getZone(
        hr: Int,
        baseline: Int,
        age: Int,
        config: PointsConfig = PointsConfig()
    ): ExerciseZone {

        val upperCap =
            calculateUpperHeartRateCap(
                baseline = baseline,
                age = age,
                config = config
            )

        if (hr > upperCap) {
            return ExerciseZone.ABOVE_CAP
        }

        val aboveBaseline =
            hr - baseline

        return when {
            aboveBaseline < 10 ->
                ExerciseZone.BELOW_TARGET

            aboveBaseline < 20 ->
                ExerciseZone.LOW

            aboveBaseline < 30 ->
                ExerciseZone.MODERATE

            else ->
                ExerciseZone.INTENSE
        }
    }

    fun calculateUpperHeartRateCap(
        baseline: Int,
        age: Int,
        config: PointsConfig = PointsConfig()
    ): Int {

        val estimatedMaxHeartRate =
            220 - age

        val heartRateReserve =
            estimatedMaxHeartRate -
                baseline

        return (
            baseline +
                (
                    heartRateReserve *
                        config.upperHeartRateReserveFraction
                    )
            ).roundToInt()
    }

    fun calculateZonePoints(
        seconds: Int,
        multiplier: Double,
        config: PointsConfig = PointsConfig()
    ): Double {

        val minutes =
            seconds / 60.0

        return minutes *
            config.basePointsPerMinute *
            multiplier
    }

    fun checkHeartRateReading(
        heartRate: Int,
        previousHeartRate: Int?,
        age: Int,
        secondsSinceLastReading: Long = 0,
        config: PointsConfig = PointsConfig()
    ): HeartRateCheck {

        if (age !in 16..80) {
            return HeartRateCheck(
                isValid = false,
                warning = HeartRateWarning.INVALID_READING,
                message = "User age is outside the supported range."
            )
        }

        if (heartRate <= 0) {
            return HeartRateCheck(
                isValid = false,
                warning = HeartRateWarning.INVALID_READING,
                message = "Heart rate reading is invalid."
            )
        }

        if (
            heartRate <
            config.minimumValidHeartRate
        ) {
            return HeartRateCheck(
                isValid = false,
                warning = HeartRateWarning.UNUSUALLY_LOW,
                message = "Heart rate reading is unusually low."
            )
        }

        val estimatedMaxHeartRate =
            220 - age

        if (
            heartRate >
            estimatedMaxHeartRate
        ) {
            return HeartRateCheck(
                isValid = false,
                warning = HeartRateWarning.ABOVE_ESTIMATED_MAX,
                message = "Heart rate is above the expected range."
            )
        }

        if (previousHeartRate != null) {

            val difference =
                abs(
                    heartRate -
                        previousHeartRate
                )

            if (
                difference >
                config.maximumSuddenChangeBpm
            ) {
                return HeartRateCheck(
                    isValid = false,
                    warning = HeartRateWarning.SUDDEN_CHANGE,
                    message = "Heart rate changed unusually quickly."
                )
            }
        }

        if (
            secondsSinceLastReading >
            config.staleReadingSeconds
        ) {
            return HeartRateCheck(
                isValid = false,
                warning = HeartRateWarning.STALE_READING,
                message = "Heart rate data has stopped updating."
            )
        }

        return HeartRateCheck(
            isValid = true,
            warning = HeartRateWarning.NONE
        )
    }
}