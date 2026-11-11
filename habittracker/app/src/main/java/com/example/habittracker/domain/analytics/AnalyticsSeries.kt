package com.example.habittracker.domain.analytics

import com.example.habittracker.domain.model.DailyActivity
import java.time.LocalDate

/**
 * Дополняет историю пустыми днями, чтобы график не «прыгал»:
 * пользователь видит весь выбранный диапазон, даже если часть дней без отметок.
 */
fun buildDailySeries(
    today: LocalDate,
    days: Int,
    byDay: Map<LocalDate, DailyActivity>,
): List<DailyActivity> {
    if (days <= 0) return emptyList()
    return (days - 1 downTo 0).map { offset ->
        val day = today.minusDays(offset.toLong())
        byDay[day] ?: DailyActivity(
            day = day,
            completions = 0,
            misses = 0,
            xpEarned = 0,
            goldEarned = 0,
        )
    }
}
