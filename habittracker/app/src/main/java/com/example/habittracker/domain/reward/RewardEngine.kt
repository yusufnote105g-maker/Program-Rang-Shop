package com.example.habittracker.domain.reward

import com.example.habittracker.domain.model.Difficulty
import kotlin.math.max

/**
 * Математика наград. Чистые функции — легко тестировать, легко заменить.
 * Формулы наши собственные.
 */
object RewardEngine {

    /** Сколько опыта нужно, чтобы закрыть уровень [level]. */
    fun xpToNext(level: Int): Int = 100 + (level - 1) * 50

    data class XpApplied(val level: Int, val xp: Int, val leveledUp: Boolean)

    /** Начисляет (или списывает) опыт, поднимая уровень столько раз, сколько нужно. */
    fun applyXp(level: Int, xp: Int, delta: Int): XpApplied {
        var lvl = level
        var cur = xp + delta
        var leveled = false
        while (cur >= xpToNext(lvl)) {
            cur -= xpToNext(lvl)
            lvl += 1
            leveled = true
        }
        if (cur < 0) cur = 0
        return XpApplied(lvl, cur, leveled)
    }

    fun rewardFor(difficulty: Difficulty): Pair<Int, Int> = difficulty.xp to difficulty.gold

    fun penaltyFor(difficulty: Difficulty): Pair<Int, Int> = difficulty.xp to difficulty.hpPenalty

    fun clampHp(hp: Int, maxHp: Int): Int = max(0, hp.coerceAtMost(maxHp))
}
