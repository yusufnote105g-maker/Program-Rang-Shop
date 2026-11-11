package com.example.habittracker.domain.model

/** Что показать в уведомлении и когда напомнить в следующий раз. */
data class ReminderFiring(
    val taskId: String,
    val text: String,
    val notes: String,
    val nextTriggerAt: Long?,
)
