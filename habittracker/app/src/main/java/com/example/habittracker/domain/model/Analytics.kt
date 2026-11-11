package com.example.habittracker.domain.model

import java.time.LocalDate

/** Один день истории для графика. Все поля — за локальный день пользователя. */
data class DailyActivity(
    val day: LocalDate,
    val completions: Int,
    val misses: Int,
    val xpEarned: Int,
    val goldEarned: Int,
)

data class TaskTypeCount(
    val type: TaskType,
    val total: Int,
    val completed: Int,
)

data class StreakItem(
    val id: String,
    val text: String,
    val current: Int,
    val best: Int,
)

/** Снимок для экрана аналитики. Считается из task_logs и tasks. */
data class AnalyticsData(
    val rangeDays: Int,
    val daily: List<DailyActivity>,
    val totalCompletions: Int,
    val totalMisses: Int,
    val xpEarned: Int,
    val goldEarned: Int,
    val counts: List<TaskTypeCount>,
    val topStreaks: List<StreakItem>,
    val level: Int,
    val gold: Int,
) {
    val hasHistory: Boolean get() = totalCompletions > 0 || totalMisses > 0
}
