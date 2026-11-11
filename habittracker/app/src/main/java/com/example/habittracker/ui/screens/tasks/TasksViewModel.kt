package com.example.habittracker.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.repository.TaskRepository
import com.example.habittracker.domain.model.RewardResult
import com.example.habittracker.domain.model.Task
import com.example.habittracker.domain.model.TaskType
import com.example.habittracker.domain.model.UserStats
import dagger.hilt.android.lifecycle.HiltViewModel
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
            TasksUiState(
                selectedTab = tab,
                tasks = tasks,
                stats = stats,
                today = today,
                loading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TasksUiState(),
        )

    init {
        viewModelScope.launch {
            repository.ensureUser()
            // Догоняем пропущенные дни, если приложение открыли после границы дня.
            repository.runDailyCron()?.let { report ->
                if (report.missedDailies > 0) {
                    events.send(TasksEvent.DailyReset(report.missedDailies, report.hpLost))
                }
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
        viewModelScope.launch { repository.deleteTask(taskId) }
    }

    private fun runReward(block: suspend () -> RewardResult?) {
        viewModelScope.launch {
            block()?.let { events.send(TasksEvent.Reward(it)) }
        }
    }
}
