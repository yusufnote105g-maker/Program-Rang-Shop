package com.example.habittracker.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.repository.TaskRepository
import com.example.habittracker.domain.model.RewardResult
import com.example.habittracker.domain.model.Tag
import com.example.habittracker.domain.model.Task
import com.example.habittracker.domain.model.TaskType
import com.example.habittracker.domain.model.UserStats
import com.example.habittracker.domain.tags.TaskGroup
import com.example.habittracker.domain.tags.filterTasks
import com.example.habittracker.domain.tags.groupTasks
import com.example.habittracker.widget.WidgetUpdater
import com.example.habittracker.work.ReminderScheduler
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasksUiState(
    val selectedTab: TaskType = TaskType.HABIT,
    /** Отфильтрованный список; при включённой группировке UI берёт [groups]. */
    val tasks: List<Task> = emptyList(),
    val groups: List<TaskGroup> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val selectedFilters: Set<String> = emptySet(),
    val groupByTag: Boolean = false,
    val stats: UserStats? = null,
    val today: String = "",
    val loading: Boolean = true,
)

/** Разовые сообщения для экрана (snackbar). */
sealed interface TasksEvent {
    data class Reward(val result: RewardResult) : TasksEvent
    data class DailyReset(val missed: Int, val hpLost: Int) : TasksEvent
    data object Deleted : TasksEvent
    data object Duplicated : TasksEvent
    data object Moved : TasksEvent

    /** Отметка/удаление не применились: например, задача уже удалена или БД недоступна. */
    data object ActionFailed : TasksEvent
    data object DeleteFailed : TasksEvent
    data object DuplicateFailed : TasksEvent
    data object MoveFailed : TasksEvent
}

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val reminderScheduler: ReminderScheduler,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    private val selectedTab = MutableStateFlow(TaskType.HABIT)
    private val selectedFilters = MutableStateFlow<Set<String>>(emptySet())
    private val groupByTag = MutableStateFlow(false)

    private val events = Channel<TasksEvent>(Channel.BUFFERED)
    val uiEvents = events.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val tasksFlow = selectedTab.flatMapLatest { repository.observeTasks(it) }

    private data class BaseState(
        val tab: TaskType,
        val tasks: List<Task>,
        val stats: UserStats?,
        val today: String,
        val tags: List<Tag>,
        val loading: Boolean,
    )

    private val baseFlow = combine(
        selectedTab,
        tasksFlow,
        repository.observeUser(),
        repository.observeToday(),
        repository.observeTags(),
    ) { tab, tasks, stats, today, tags ->
        // Сразу после переключения вкладки flatMapLatest ещё не успел отдать
        // новый список; не показываем чужие задачи под новым табом.
        val belongsToTab = tasks.all { it.type == tab }
        BaseState(
            tab = tab,
            tasks = if (belongsToTab) tasks else emptyList(),
            stats = stats,
            today = today,
            tags = tags,
            loading = !belongsToTab,
        )
    }

    val uiState: StateFlow<TasksUiState> =
        combine(baseFlow, selectedFilters, groupByTag) { base, filters, grouped ->
            val visible = filterTasks(base.tasks, filters)
            TasksUiState(
                selectedTab = base.tab,
                tasks = visible,
                groups = groupTasks(visible),
                availableTags = base.tags,
                selectedFilters = filters,
                groupByTag = grouped,
                stats = base.stats,
                today = base.today,
                loading = base.loading,
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

    fun toggleTagFilter(name: String) {
        selectedFilters.update { current ->
            if (current.contains(name)) current - name else current + name
        }
    }

    fun clearTagFilters() {
        selectedFilters.value = emptySet()
    }

    fun toggleGrouping() {
        groupByTag.update { !it }
    }

    fun tapPlus(taskId: String) = runReward { repository.tapPlus(taskId) }

    fun tapMinus(taskId: String) = runReward { repository.tapMinus(taskId) }

    fun completeTodo(taskId: String) = runReward(onSuccess = {
        reminderScheduler.cancel(taskId)
    }) {
        repository.completeTodo(taskId)
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                repository.deleteTask(taskId)
                reminderScheduler.cancel(taskId)
                widgetUpdater.refresh()
                events.send(TasksEvent.Deleted)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                events.send(TasksEvent.DeleteFailed)
            }
        }
    }

    fun duplicateTask(taskId: String) {
        viewModelScope.launch {
            try {
                val copy = repository.duplicateTask(taskId)
                if (copy == null) {
                    events.send(TasksEvent.ActionFailed)
                    return@launch
                }
                copy.reminderAt?.let { trigger ->
                    reminderScheduler.schedule(copy.id, trigger, copy.text)
                }
                widgetUpdater.refresh()
                events.send(TasksEvent.Duplicated)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                events.send(TasksEvent.DuplicateFailed)
            }
        }
    }

    fun moveTask(taskId: String, type: TaskType) {
        viewModelScope.launch {
            try {
                if (!repository.moveTask(taskId, type)) {
                    events.send(TasksEvent.ActionFailed)
                    return@launch
                }
                val moved = repository.loadTask(taskId)
                if (moved != null && moved.reminderEnabled && moved.reminderAt != null) {
                    reminderScheduler.schedule(taskId, moved.reminderAt, moved.text)
                } else {
                    reminderScheduler.cancel(taskId)
                }
                widgetUpdater.refresh()
                events.send(TasksEvent.Moved)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                events.send(TasksEvent.MoveFailed)
            }
        }
    }

    private fun runReward(
        onSuccess: () -> Unit = {},
        block: suspend () -> RewardResult?,
    ) {
        viewModelScope.launch {
            try {
                block()?.let {
                    onSuccess()
                    widgetUpdater.refresh()
                    events.send(TasksEvent.Reward(it))
                }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                // Ошибку БД не роняем в UI-поток: показываем snackbar.
                events.send(TasksEvent.ActionFailed)
            }
        }
    }
}
