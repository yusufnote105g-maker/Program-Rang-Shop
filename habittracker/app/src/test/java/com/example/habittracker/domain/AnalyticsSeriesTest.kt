package com.example.habittracker.domain

import com.example.habittracker.domain.analytics.buildDailySeries
import com.example.habittracker.domain.model.DailyActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AnalyticsSeriesTest {

    private val today = LocalDate.of(2026, 11, 10)

    @Test
    fun buildDailySeries_fillsMissingDaysInOrder() {
        val series = buildDailySeries(
            today = today,
            days = 3,
            byDay = mapOf(
                LocalDate.of(2026, 11, 9) to DailyActivity(
                    day = LocalDate.of(2026, 11, 9),
                    completions = 2,
                    misses = 1,
                    xpEarned = 8,
                    goldEarned = 4,
                ),
            ),
        )
        assertEquals(3, series.size)
        assertEquals(LocalDate.of(2026, 11, 8), series.first().day)
        assertEquals(0, series.first().completions)
        assertEquals(2, series[1].completions)
        assertEquals(today, series.last().day)
    }

    @Test
    fun buildDailySeries_returnsEmptyForNonPositiveRange() {
        assertTrue(buildDailySeries(today, 0, emptyMap()).isEmpty())
        assertTrue(buildDailySeries(today, -5, emptyMap()).isEmpty())
    }

    @Test
    fun buildDailySeries_keepsTodayWhenRangeIsOne() {
        val series = buildDailySeries(today, 1, emptyMap())
        assertEquals(listOf(today), series.map { it.day })
        assertEquals(0, series.single().completions)
    }
}
