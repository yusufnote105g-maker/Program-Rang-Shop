package com.example.habittracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.habittracker.data.repository.TaskRepository
import com.example.habittracker.work.DailyResetScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class HabitApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var repository: TaskRepository
    @Inject lateinit var resetScheduler: DailyResetScheduler

    /** Живёт столько же, сколько процесс: работа при старте приложения. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            // WorkManager не гарантирует точное время (Doze, экономия батареи),
            // поэтому при каждом запуске догоняем пропущенные дни и пересчитываем
            // задержку до следующей границы дня.
            repository.ensureUser()
            repository.runDailyCron()
            resetScheduler.schedule()
        }
    }
}
