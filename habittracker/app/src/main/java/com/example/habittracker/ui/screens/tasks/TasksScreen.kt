package com.example.habittracker.ui.screens.tasks

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.habittracker.R
import com.example.habittracker.domain.model.Tag
import com.example.habittracker.domain.model.Task
import com.example.habittracker.domain.model.TaskType
import com.example.habittracker.domain.tags.TaskGroup
import com.example.habittracker.ui.components.AvatarHeader
import com.example.habittracker.ui.components.ConfirmDeleteDialog
import com.example.habittracker.ui.components.EmptyState
import com.example.habittracker.ui.components.MoveTaskDialog
import com.example.habittracker.ui.components.TagDot
import com.example.habittracker.ui.components.TaskListSkeleton
import com.example.habittracker.ui.components.TaskRow
import com.example.habittracker.ui.theme.Dimens

/** S01: главный список задач. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onAddTask: (TaskType) -> Unit,
    onEditTask: (String) -> Unit,
    onOpenAnalytics: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingMoveId by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingDelete = state.tasks.firstOrNull { it.id == pendingDeleteId }
    val pendingMove = state.tasks.firstOrNull { it.id == pendingMoveId }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            val message = when (event) {
                is TasksEvent.Reward -> {
                    val reward = event.result
                    when {
                        reward.leveledUp -> context.getString(R.string.reward_level_up, reward.newLevel)
                        reward.hpDelta < 0 && reward.xpDelta < 0 ->
                            context.getString(R.string.reward_penalty_both, -reward.xpDelta, -reward.hpDelta)
                        reward.hpDelta < 0 -> context.getString(R.string.reward_penalty, -reward.hpDelta)
                        reward.xpDelta < 0 -> context.getString(R.string.reward_xp_loss, -reward.xpDelta)
                        else -> context.getString(R.string.reward_xp, reward.xpDelta)
                    }
                }

                is TasksEvent.DailyReset ->
                    if (event.hpLost > 0) {
                        context.getString(R.string.cron_missed, event.missed, event.hpLost)
                    } else {
                        context.getString(R.string.cron_missed_no_hp, event.missed)
                    }

                TasksEvent.Deleted -> context.getString(R.string.task_deleted)
                TasksEvent.Duplicated -> context.getString(R.string.task_duplicated)
                TasksEvent.Moved -> context.getString(R.string.task_moved)
                TasksEvent.DeleteFailed -> context.getString(R.string.delete_failed)
                TasksEvent.DuplicateFailed -> context.getString(R.string.duplicate_failed)
                TasksEvent.MoveFailed -> context.getString(R.string.move_failed)
                TasksEvent.ActionFailed -> context.getString(R.string.action_failed)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenAnalytics) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = stringResource(R.string.analytics_open),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Тип новой задачи наследуется от открытой вкладки — так «+» на вкладке
            // «Ежедневные» не открывает форму привычки.
            FloatingActionButton(onClick = { onAddTask(state.selectedTab) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_task))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            AvatarHeader(
                stats = state.stats,
                modifier = Modifier.padding(horizontal = Dimens.screenPadding),
            )
            Spacer(Modifier.height(Dimens.md))
            TaskTabs(selected = state.selectedTab, onSelect = viewModel::selectTab)
            Spacer(Modifier.height(Dimens.sm))
            if (state.availableTags.isNotEmpty()) {
                TagFilterBar(
                    tags = state.availableTags,
                    selected = state.selectedFilters,
                    groupByTag = state.groupByTag,
                    onToggle = viewModel::toggleTagFilter,
                    onClear = viewModel::clearTagFilters,
                    onToggleGrouping = viewModel::toggleGrouping,
                )
                Spacer(Modifier.height(Dimens.sm))
            }

            when {
                state.loading -> TaskListSkeleton()
                state.tasks.isEmpty() -> {
                    if (state.selectedFilters.isNotEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.filter_empty_title),
                            body = stringResource(R.string.filter_empty_body),
                            cta = stringResource(R.string.filter_reset),
                            onCta = viewModel::clearTagFilters,
                        )
                    } else {
                        val copy = emptyCopyFor(state.selectedTab)
                        EmptyState(
                            title = copy.title,
                            body = copy.body,
                            cta = copy.cta,
                            onCta = { onAddTask(state.selectedTab) },
                        )
                    }
                }

                state.groupByTag -> GroupedTaskList(
                    groups = state.groups,
                    today = state.today,
                    onPlus = viewModel::tapPlus,
                    onMinus = viewModel::tapMinus,
                    onComplete = viewModel::completeTodo,
                    onEdit = onEditTask,
                    onDelete = { pendingDeleteId = it },
                    onDuplicate = viewModel::duplicateTask,
                    onMove = { pendingMoveId = it },
                )

                else -> FlatTaskList(
                    tasks = state.tasks,
                    today = state.today,
                    onPlus = viewModel::tapPlus,
                    onMinus = viewModel::tapMinus,
                    onComplete = viewModel::completeTodo,
                    onEdit = onEditTask,
                    onDelete = { pendingDeleteId = it },
                    onDuplicate = viewModel::duplicateTask,
                    onMove = { pendingMoveId = it },
                )
            }
        }
    }

    val taskToDelete = pendingDelete
    if (taskToDelete != null) {
        ConfirmDeleteDialog(
            taskName = taskToDelete.text,
            onConfirm = {
                pendingDeleteId = null
                viewModel.deleteTask(taskToDelete.id)
            },
            onDismiss = { pendingDeleteId = null },
        )
    }

    val taskToMove = pendingMove
    if (taskToMove != null) {
        MoveTaskDialog(
            task = taskToMove,
            onConfirm = { target ->
                pendingMoveId = null
                viewModel.moveTask(taskToMove.id, target)
            },
            onDismiss = { pendingMoveId = null },
        )
    }
}

@Composable
private fun FlatTaskList(
    tasks: List<Task>,
    today: String,
    onPlus: (String) -> Unit,
    onMinus: (String) -> Unit,
    onComplete: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onMove: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.rowGap),
    ) {
        items(items = tasks, key = { it.id }) { task ->
            TaskRowItem(
                task = task,
                today = today,
                onPlus = onPlus,
                onMinus = onMinus,
                onComplete = onComplete,
                onEdit = onEdit,
                onDelete = onDelete,
                onDuplicate = onDuplicate,
                onMove = onMove,
            )
        }
    }
}

@Composable
private fun GroupedTaskList(
    groups: List<TaskGroup>,
    today: String,
    onPlus: (String) -> Unit,
    onMinus: (String) -> Unit,
    onComplete: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onMove: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.rowGap),
    ) {
        groups.forEach { group ->
            item(key = "header:${group.tag?.id ?: "none"}") {
                GroupHeader(group = group)
            }
            items(
                items = group.tasks,
                key = { task -> "${group.tag?.id ?: "none"}:${task.id}" },
            ) { task ->
                TaskRowItem(
                    task = task,
                    today = today,
                    onPlus = onPlus,
                    onMinus = onMinus,
                    onComplete = onComplete,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onDuplicate = onDuplicate,
                    onMove = onMove,
                )
            }
        }
    }
}

@Composable
private fun TaskRowItem(
    task: Task,
    today: String,
    onPlus: (String) -> Unit,
    onMinus: (String) -> Unit,
    onComplete: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onMove: (String) -> Unit,
) {
    TaskRow(
        task = task,
        today = today,
        onPlus = { onPlus(task.id) },
        onMinus = { onMinus(task.id) },
        onComplete = { onComplete(task.id) },
        onEdit = { onEdit(task.id) },
        onDelete = { onDelete(task.id) },
        onDuplicate = { onDuplicate(task.id) },
        onMove = { onMove(task.id) },
    )
}

@Composable
private fun GroupHeader(group: TaskGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.sm, bottom = Dimens.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        if (group.tag != null) {
            TagDot(color = group.tag.color)
        } else {
            Icon(
                imageVector = Icons.Default.Label,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.height(Dimens.iconSm),
            )
        }
        Text(
            text = group.tag?.name ?: stringResource(R.string.group_untagged),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.group_count, group.tasks.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TagFilterBar(
    tags: List<Tag>,
    selected: Set<String>,
    groupByTag: Boolean,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    onToggleGrouping: () -> Unit,
) {
    val rowA11y = stringResource(R.string.filter_by_tag)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Dimens.screenPadding)
            .semantics { contentDescription = rowA11y },
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = selected.isEmpty(),
            onClick = onClear,
            label = { Text(stringResource(R.string.filter_all)) },
        )
        tags.forEach { tag ->
            FilterChip(
                selected = tag.name in selected,
                onClick = { onToggle(tag.name) },
                label = { Text(tag.name) },
                leadingIcon = { TagDot(color = tag.color) },
            )
        }
        FilterChip(
            selected = groupByTag,
            onClick = onToggleGrouping,
            label = { Text(stringResource(R.string.group_by_tag)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.ViewAgenda,
                    contentDescription = null,
                    modifier = Modifier.height(Dimens.iconSm),
                )
            },
        )
    }
}

private data class EmptyCopy(val title: String, val body: String, val cta: String)

@Composable
private fun emptyCopyFor(tab: TaskType): EmptyCopy = when (tab) {
    TaskType.HABIT -> EmptyCopy(
        title = stringResource(R.string.empty_habits_title),
        body = stringResource(R.string.empty_habits_body),
        cta = stringResource(R.string.empty_habits_cta),
    )

    TaskType.DAILY -> EmptyCopy(
        title = stringResource(R.string.empty_dailies_title),
        body = stringResource(R.string.empty_dailies_body),
        cta = stringResource(R.string.empty_dailies_cta),
    )

    TaskType.TODO -> EmptyCopy(
        title = stringResource(R.string.empty_todos_title),
        body = stringResource(R.string.empty_todos_body),
        cta = stringResource(R.string.empty_todos_cta),
    )
}

@Composable
private fun TaskTabs(selected: TaskType, onSelect: (TaskType) -> Unit) {
    val tabs = TaskType.entries
    TabRow(selectedTabIndex = tabs.indexOf(selected)) {
        tabs.forEach { type ->
            Tab(
                selected = type == selected,
                onClick = { onSelect(type) },
                text = { Text(tabLabel(type)) },
            )
        }
    }
}

@Composable
private fun tabLabel(type: TaskType): String = stringResource(
    when (type) {
        TaskType.HABIT -> R.string.tab_habits
        TaskType.DAILY -> R.string.tab_dailies
        TaskType.TODO -> R.string.tab_todos
    },
)
