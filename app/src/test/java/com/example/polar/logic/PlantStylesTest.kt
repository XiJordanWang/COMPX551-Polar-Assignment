package com.example.polar.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class PlantStylesTest {

    private fun ids(points: Int) = unlockedStyles(points).map { it.id }

    @Test
    fun zeroPoints_onlyClassic() {
        assertEquals(listOf("classic"), ids(0))
    }

    @Test
    fun unlocksExactlyAtTheThresholds() {
        assertEquals(listOf("classic"), ids(1_999))
        assertEquals(listOf("classic", "sakura"), ids(2_000))
        assertEquals(listOf("classic", "sakura"), ids(4_999))
        assertEquals(listOf("classic", "sakura", "ocean"), ids(5_000))
        assertEquals(listOf("classic", "sakura", "ocean", "golden"), ids(10_000))
    }

    @Test
    fun savedStyle_usedOnlyIfUnlocked() {
        assertEquals("ocean", usableStyle("ocean", totalPoints = 6_000).id)
        assertEquals("classic", usableStyle("golden", totalPoints = 6_000).id)   // locked -> Classic
    }

    @Test
    fun unknownOrMissingStyle_fallsBackToClassic() {
        assertEquals("classic", usableStyle(null, totalPoints = 20_000).id)
        assertEquals("classic", usableStyle("rainbow", totalPoints = 20_000).id)
    }
}
