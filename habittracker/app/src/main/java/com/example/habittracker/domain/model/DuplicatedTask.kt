package com.example.habittracker.domain.model

/** Новая задача после «Дублировать»: id и ближайшее напоминание нужны для планировщика. */
data class DuplicatedTask(
    val id: String,
    val text: String,
    val reminderAt: Long?,
)
