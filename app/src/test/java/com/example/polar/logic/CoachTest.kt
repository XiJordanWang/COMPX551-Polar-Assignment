package com.example.polar.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

// Runs on the computer, no phone needed:
//   ./gradlew :app:testDebugUnitTest
class CoachTest {

    @Test
    fun offMode_givesNoMessage() {
        for (trigger in CoachTrigger.entries) {
            assertNull(pickMessage(CoachMode.OFF, trigger, Random(1), lastMessage = null))
        }
    }

    @Test
    fun neverRepeatsTheLastMessage() {
        // Ask 200 times in a row and check each message is different from the one before
        for (mode in listOf(CoachMode.SUPPORTIVE, CoachMode.BULLY, CoachMode.MIXED)) {
            val random = Random(1)
            var last: String? = null
            repeat(200) {
                val message = pickMessage(mode, CoachTrigger.INACTIVE, random, last)
                assertNotEquals(last, message)
                last = message
            }
        }
    }

    @Test
    fun mixedMode_canReturnBothKinds() {
        val random = Random(7)
        val seen = mutableSetOf<String>()
        repeat(100) {
            val message = pickMessage(CoachMode.MIXED, CoachTrigger.GOAL_REACHED, random, lastMessage = null)
            if (message != null) seen.add(message)
        }
        val supportive = supportiveMessages[CoachTrigger.GOAL_REACHED]!!
        val bully = bullyMessages[CoachTrigger.GOAL_REACHED]!!
        assertTrue("MIXED should give at least one supportive message", seen.any { it in supportive })
        assertTrue("MIXED should give at least one bully message", seen.any { it in bully })
    }

    @Test
    fun supportiveMode_onlyGivesSupportiveMessages() {
        val random = Random(3)
        val supportive = supportiveMessages[CoachTrigger.STREAK_AT_RISK]!!
        repeat(50) {
            val message = pickMessage(CoachMode.SUPPORTIVE, CoachTrigger.STREAK_AT_RISK, random, lastMessage = null)
            assertTrue(message in supportive)
        }
    }

    @Test
    fun bullyMode_onlyGivesBullyMessages() {
        val random = Random(3)
        val bully = bullyMessages[CoachTrigger.INACTIVE]!!
        repeat(50) {
            val message = pickMessage(CoachMode.BULLY, CoachTrigger.INACTIVE, random, lastMessage = null)
            assertTrue(message in bully)
        }
    }

    @Test
    fun sameSeed_givesSameMessage() {
        // This is why random is a parameter: the result is repeatable
        val first = pickMessage(CoachMode.MIXED, CoachTrigger.INACTIVE, Random(42), lastMessage = null)
        val second = pickMessage(CoachMode.MIXED, CoachTrigger.INACTIVE, Random(42), lastMessage = null)
        assertEquals(first, second)
    }

    @Test
    fun everyModeAndTrigger_hasFourMessages() {
        for (trigger in CoachTrigger.entries) {
            assertEquals(4, supportiveMessages[trigger]?.size)
            assertEquals(4, bullyMessages[trigger]?.size)
        }
    }
}
