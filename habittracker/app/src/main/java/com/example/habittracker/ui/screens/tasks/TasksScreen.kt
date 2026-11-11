package com.example.habittracker.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.habittracker.R
import com.example.habittracker.domain.model.TaskType
import com.example.habittracker.ui.components.AvatarHeader
import com.example.habittracker.ui.components.EmptyState
import com.example.habittracker.ui.components.TaskRow
import com.example.habittracker.ui.theme.Dimens

/** S01: главный список задач. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onAddTask: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            val message = when (event) {
                is TasksEvent.Reward -> {
                    val reward = event.result
                    when {
                        reward.leveledUp -> context.getString(R.string.reward_level_up, reward.newLevel)
                        reward.hpDelta < 0 -> context.getString(R.string.reward_penalty, -reward.hpDelta)
                        else -> context.getString(R.string.reward_xp, reward.xpDelta)
                    }
                }

                is TasksEvent.DailyReset ->
                    context.getString(R.string.cron_missed, event.missed, event.hpLost)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
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

            if (state.tasks.isEmpty() && !state.loading) {
                EmptyState(
                    title = stringResource(R.string.empty_habits_title),
                    body = stringResource(R.string.empty_habits_body),
                    cta = stringResource(R.string.empty_cta),
                    onCta = onAddTask,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Dimens.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.rowGap),
                ) {
                    items(items = state.tasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            today = state.today,
                            onPlus = { viewModel.tapPlus(task.id) },
                            onMinus = { viewModel.tapMinus(task.id) },
                            onComplete = { viewModel.completeTodo(task.id) },
                        )
                    }
                }
            }
        }
    }
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
