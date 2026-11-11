package com.example.habittracker.domain.model

/** Данные, которые домашний виджет показывает без открытия приложения. */
data class WidgetSnapshot(
    val level: Int,
    val xp: Int,
    val xpToNext: Int,
    val hp: Int,
    val maxHp: Int,
    val gold: Int,
    val habits: Int,
    val dailiesDue: Int,
    val dailiesDone: Int,
    val todosOpen: Int,
    val bestStreak: Int,
    val bestStreakText: String?,
)
