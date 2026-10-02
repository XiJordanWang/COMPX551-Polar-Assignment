package com.example.polar.logic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InactivityDetectorTest {

    private val baseline = 70

    @Test
    fun allInsideTheTolerance_isInactive() {
        val hr = List(180) { if (it % 2 == 0) 68 else 73 }
        assertTrue(isInactive(hr, baseline, windowSec = 180, tolerance = 5))
    }

    @Test
    fun oneValueOutside_isNotInactive() {
        val hr = MutableList(180) { 70 }
        hr[100] = 76   // +6
        assertFalse(isInactive(hr, baseline, windowSec = 180, tolerance = 5))
    }

    @Test
    fun tooFewSamples_isNotInactive() {
        assertFalse(isInactive(List(179) { 70 }, baseline, windowSec = 180))
        assertFalse(isInactive(emptyList(), baseline, windowSec = 180))
    }

    @Test
    fun exactlyPlusOrMinus5_countsAsInside() {
        val hr = List(30) { if (it % 2 == 0) 65 else 75 }
        assertTrue(isInactive(hr, baseline, windowSec = 30, tolerance = 5))
    }

    @Test
    fun onlyTheLastWindowCounts() {
        // Moving hard first, then resting for the last 30 seconds -> inactive now
        val hr = List(60) { 140 } + List(30) { 71 }
        assertTrue(isInactive(hr, baseline, windowSec = 30))
    }

    @Test
    fun cooldown_blocksForFiveMinutes() {
        val sent = 1_000_000L
        assertTrue(canSendAgain(lastSentAt = 0L, now = sent))                       // never sent
        assertFalse(canSendAgain(lastSentAt = sent, now = sent + 60_000))            // 1 min later
        assertTrue(canSendAgain(lastSentAt = sent, now = sent + COACH_COOLDOWN_MS))  // 5 min later
    }
}
