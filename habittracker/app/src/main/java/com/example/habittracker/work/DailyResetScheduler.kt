package com.example.habittracker.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.habittracker.data.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ставит ежедневную задачу WorkManager так, чтобы она срабатывала на границе дня
 * пользователя (его зона + `day_start_minutes`), а не в полночь по UTC.
 *
 * Точность best-effort: Android может отложить работу (Doze, экономия батареи),
 * поэтому приложение дополнительно догоняет сброс при каждом старте.
 */
@Singleton
class DailyResetScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TaskRepository,
) {

    suspend fun schedule() {
        val initialDelay = repository.millisUntilNextDayBoundary()
        val request = PeriodicWorkRequestBuilder<DailyResetWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NAME,
            // UPDATE: пересчитываем задержку, если пользователь сменил зону или границу дня.
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    companion object {
        const val NAME = "daily-reset"
        const val TAG = "daily-reset"
    }
}
