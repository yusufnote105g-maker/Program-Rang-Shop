package com.example.habittracker.di

import com.example.habittracker.data.repository.TaskRepository
import com.example.habittracker.work.ReminderScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Мост в Hilt для фоновых BroadcastReceiver'ов (напоминания, виджет, boot).
 * Receiver'ы не создают ViewModel и не должны держать ссылку на Activity.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackgroundEntryPoint {
    fun taskRepository(): TaskRepository
    fun reminderScheduler(): ReminderScheduler
}
