package com.example.habittracker.domain

import com.example.habittracker.domain.cron.CronEngine
import com.example.habittracker.domain.cron.TaskSchedule
import com.example.habittracker.domain.model.Frequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Календарная логика F06: due-дни, предыдущий нужный день, серия. */
class CronEngineTest {

    private val start = LocalDate.of(2026, 11, 1)
    private val daily = TaskSchedule(start, Frequency.DAILY)

    // 2026-11-02 — понедельник, 11-04 — среда, 11-06 — пятница.
    private val monWedFri = TaskSchedule(start, Frequency.WEEKLY, repeatDays = setOf(0, 2, 4))

    @Test
    fun daily_isDueEveryDayAfterStart() {
        assertFalse(CronEngine.isDue(daily, LocalDate.of(2026, 10, 31)))
        assertTrue(CronEngine.isDue(daily, start))
        assertTrue(CronEngine.isDue(daily, LocalDate.of(2026, 11, 10)))
    }

    @Test
    fun everyX_skipsDaysBetween() {
        val everyTwo = TaskSchedule(start, Frequency.DAILY, everyX = 2)
        assertTrue(CronEngine.isDue(everyTwo, LocalDate.of(2026, 11, 1)))
        assertFalse(CronEngine.isDue(everyTwo, LocalDate.of(2026, 11, 2)))
        assertTrue(CronEngine.isDue(everyTwo, LocalDate.of(2026, 11, 3)))
    }

    @Test
    fun weekly_onlyOnSelectedDays() {
        assertTrue(CronEngine.isDue(monWedFri, LocalDate.of(2026, 11, 2)))
        assertFalse(CronEngine.isDue(monWedFri, LocalDate.of(2026, 11, 3)))
        assertTrue(CronEngine.isDue(monWedFri, LocalDate.of(2026, 11, 4)))
    }

    @Test
    fun monthly_31st_fallsBackToLastDayOfShortMonth() {
        val monthly = TaskSchedule(LocalDate.of(2026, 1, 31), Frequency.MONTHLY)
        assertTrue(CronEngine.isDue(monthly, LocalDate.of(2026, 2, 28)))
        assertFalse(CronEngine.isDue(monthly, LocalDate.of(2026, 2, 27)))
        assertTrue(CronEngine.isDue(monthly, LocalDate.of(2026, 3, 31)))
    }

    @Test
    fun previousDueDay_skipsNonScheduledDays() {
        // Смотрим «назад» от пятницы 11-06: предыдущая нужная — среда 11-04.
        assertEquals(
            LocalDate.of(2026, 11, 4),
            CronEngine.previousDueDay(monWedFri, LocalDate.of(2026, 11, 6)),
        )
    }

    @Test
    fun streak_continues_whenPreviousDueDayWasClosed() {
        val streak = CronEngine.streakAfterCompletion(
            currentStreak = 5,
            lastCompletedDay = LocalDate.of(2026, 11, 9),
            schedule = daily,
            today = LocalDate.of(2026, 11, 10),
        )
        assertEquals(6, streak)
    }

    @Test
    fun streak_restartsAfterGap() {
        val streak = CronEngine.streakAfterCompletion(
            currentStreak = 5,
            lastCompletedDay = LocalDate.of(2026, 11, 7),
            schedule = daily,
            today = LocalDate.of(2026, 11, 10),
        )
        assertEquals(1, streak)
    }

    @Test
    fun streak_startsAtOne_whenNothingCompletedYet() {
        val streak = CronEngine.streakAfterCompletion(
            currentStreak = 0,
            lastCompletedDay = null,
            schedule = daily,
            today = LocalDate.of(2026, 11, 10),
        )
        assertEquals(1, streak)
    }

    @Test
    fun previousDueDay_isNull_whenTaskStartsToday() {
        val fresh = TaskSchedule(LocalDate.of(2026, 11, 10), Frequency.DAILY)
        assertNull(CronEngine.previousDueDay(fresh, LocalDate.of(2026, 11, 10)))
    }
}
