package com.example.habittracker.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.repository.TaskRepository
import com.example.habittracker.domain.model.RewardResult
import com.example.habittracker.domain.model.Task
import com.example.habittracker.domain.model.TaskType
import com.example.habittracker.domain.model.UserStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasksUiState(
    val selectedTab: TaskType = TaskType.HABIT,
    val tasks: List<Task> = emptyList(),
    val stats: UserStats? = null,
    val today: String = "",
    val loading: Boolean = true,
)

/** Разовые сообщения для экрана (snackbar). */
sealed interface TasksEvent {
    data class Reward(val result: RewardResult) : TasksEvent
    data class DailyReset(val missed: Int, val hpLost: Int) : TasksEvent
    data object Deleted : TasksEvent
    /** Отметка/удаление не применились: например, задача уже удалена или БД недоступна. */
    data object ActionFailed : TasksEvent
    data object DeleteFailed : TasksEvent
}

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TaskRepository,
) : ViewModel() {

    private val selectedTab = MutableStateFlow(TaskType.HABIT)

    private val events = Channel<TasksEvent>(Channel.BUFFERED)
    val uiEvents = events.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val tasksFlow = selectedTab.flatMapLatest { repository.observeTasks(it) }

    val uiState: StateFlow<TasksUiState> =
        combine(
            selectedTab,
            repository.observeUser(),
            tasksFlow,
            repository.observeToday(),
        ) { tab, stats, tasks, today ->
            // Сразу после переключения вкладки flatMapLatest ещё не успел отдать
            // новый список; не показываем чужие задачи под новым табом.
            val belongsToTab = tasks.all { it.type == tab }
            TasksUiState(
                selectedTab = tab,
                tasks = if (belongsToTab) tasks else emptyList(),
                stats = stats,
                today = today,
                loading = !belongsToTab,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TasksUiState(),
        )

    init {
        viewModelScope.launch {
            try {
                repository.ensureUser()
                // Догоняем пропущенные дни, если приложение открыли после границы дня.
                repository.runDailyCron()?.let { report ->
                    if (report.missedDailies > 0) {
                        events.send(TasksEvent.DailyReset(report.missedDailies, report.hpLost))
                    }
                }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                // Список всё равно покажем: сброс повторится при следующем старте или воркером.
            }
        }
    }

    fun selectTab(tab: TaskType) {
        selectedTab.value = tab
    }

    fun tapPlus(taskId: String) = runReward { repository.tapPlus(taskId) }

    fun tapMinus(taskId: String) = runReward { repository.tapMinus(taskId) }

    fun completeTodo(taskId: String) = runReward { repository.completeTodo(taskId) }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                repository.deleteTask(taskId)
                events.send(TasksEvent.Deleted)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                events.send(TasksEvent.DeleteFailed)
            }
        }
    }

    private fun runReward(block: suspend () -> RewardResult?) {
        viewModelScope.launch {
            try {
                block()?.let { events.send(TasksEvent.Reward(it)) }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                // Ошибку БД не роняем в UI-поток: показываем snackbar.
                events.send(TasksEvent.ActionFailed)
            }
        }
    }
}
