package com.example.habittracker.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.habittracker.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Фоновый пересчёт дня (F06): закрывает прошедшие ежедневные, сбрасывает серии
 * за пропуски, списывает HP и обнуляет дневные счётчики привычек.
 *
 * Идемпотентен: повторный запуск (ретрай WorkManager, перезагрузка, ручной запуск)
 * ничего не испортит — решение принимается по `users.last_cron_day`.
 */
@HiltWorker
class DailyResetWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: TaskRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        repository.ensureUser()
        repository.runDailyCron()
        Result.success()
    } catch (c: CancellationException) {
        // Работу отменили (например, приложение закрыли) — это не ошибка БД.
        throw c
    } catch (t: Throwable) {
        // Сеть не нужна, но БД может быть занята — даём WorkManager повторить.
        // Бесконечно ретраить постоянную ошибку нельзя: после нескольких попыток сдаёмся.
        if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
