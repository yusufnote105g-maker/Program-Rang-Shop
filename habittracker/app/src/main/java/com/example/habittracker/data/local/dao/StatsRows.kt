package com.example.habittracker.data.local.dao

import androidx.room.ColumnInfo

/** Проекции для аналитики и виджета. Имена колонок совпадают с SQL-алиасами. */
data class TaskTypeCountRow(
    val type: String,
    val total: Int,
    val completed: Int,
)

data class TaskStreakRow(
    val id: String,
    val text: String,
    val streak: Int,
    @ColumnInfo(name = "best_streak") val bestStreak: Int,
)

data class DailyLogRow(
    val day: String,
    val completions: Int,
    val misses: Int,
    @ColumnInfo(name = "xp_earned") val xpEarned: Int,
    @ColumnInfo(name = "gold_earned") val goldEarned: Int,
)
