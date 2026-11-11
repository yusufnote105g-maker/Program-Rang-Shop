package com.example.habittracker.domain

import com.example.habittracker.domain.model.Difficulty
import com.example.habittracker.domain.reward.RewardEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Математика наград: опыт, уровни, здоровье. */
class RewardEngineTest {

    @Test
    fun xpToNext_growsWithLevel() {
        assertEquals(100, RewardEngine.xpToNext(1))
        assertEquals(150, RewardEngine.xpToNext(2))
        assertEquals(200, RewardEngine.xpToNext(3))
    }

    @Test
    fun applyXp_levelsUpOnce() {
        val applied = RewardEngine.applyXp(level = 1, xp = 95, delta = 10)
        assertTrue(applied.leveledUp)
        assertEquals(2, applied.level)
        assertEquals(5, applied.xp)
    }

    @Test
    fun applyXp_levelsUpSeveralTimes() {
        // С 1 уровня и 0 опыта сразу 300: 100 -> 2 ур., 150 -> 3 ур., остаток 50.
        val applied = RewardEngine.applyXp(level = 1, xp = 0, delta = 300)
        assertEquals(3, applied.level)
        assertEquals(50, applied.xp)
        assertTrue(applied.leveledUp)
    }

    @Test
    fun applyXp_neverGoesNegative() {
        val applied = RewardEngine.applyXp(level = 2, xp = 3, delta = -50)
        assertEquals(0, applied.xp)
        assertFalse(applied.leveledUp)
    }

    @Test
    fun clampHp_keepsWithinBounds() {
        assertEquals(0, RewardEngine.clampHp(-10, 50))
        assertEquals(50, RewardEngine.clampHp(80, 50))
        assertEquals(20, RewardEngine.clampHp(20, 50))
    }

    @Test
    fun difficulty_scalesRewardAndPenalty() {
        val (xp, gold) = RewardEngine.rewardFor(Difficulty.HARD)
        assertEquals(Difficulty.HARD.xp, xp)
        assertEquals(Difficulty.HARD.gold, gold)
        assertTrue(Difficulty.HARD.hpPenalty >= Difficulty.TRIVIAL.hpPenalty)
    }
}
