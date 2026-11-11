package com.example.habittracker.ui.screens.taskform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.habittracker.R
import com.example.habittracker.domain.model.Difficulty
import com.example.habittracker.domain.model.Tag
import com.example.habittracker.domain.model.TaskType
import com.example.habittracker.domain.tags.TagRules
import com.example.habittracker.ui.components.ConfirmDeleteDialog
import com.example.habittracker.ui.components.TagDot
import com.example.habittracker.ui.theme.Dimens
import java.util.Locale

/** S02 — создание, S06 — редактирование. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskFormScreen(
    onDone: () -> Unit,
    viewModel: TaskFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveErrorText = stringResource(R.string.save_failed)
    val deleteErrorText = stringResource(R.string.delete_failed)
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val busy = state.saving || state.deleting

    LaunchedEffect(Unit) {
        viewModel.events.collect { onDone() }
    }

    LaunchedEffect(state.saveError) {
        if (state.saveError) {
            snackbarHostState.showSnackbar(saveErrorText)
            viewModel.clearSaveError()
        }
    }

    LaunchedEffect(state.deleteError) {
        if (state.deleteError) {
            snackbarHostState.showSnackbar(deleteErrorText)
            viewModel.clearDeleteError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEdit) R.string.edit_task_title else R.string.new_task_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone, enabled = !busy) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    if (state.isEdit && !state.loading && !state.loadFailed) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !busy,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(
                                    R.string.delete_task_a11y,
                                    state.name,
                                ),
                            )
                        }
                    }
                    TextButton(
                        onClick = viewModel::save,
                        enabled = !busy && !state.loading && !state.loadFailed,
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> FormLoading(modifier = Modifier.padding(padding))
            state.loadFailed -> FormLoadFailed(onBack = onDone, modifier = Modifier.padding(padding))
            else -> TaskFormContent(
                state = state,
                viewModel = viewModel,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (showDeleteDialog && state.isEdit) {
        ConfirmDeleteDialog(
            taskName = state.name,
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun TaskFormContent(
    state: TaskFormUiState,
    viewModel: TaskFormViewModel,
    modifier: Modifier = Modifier,
) {
    val busy = state.saving || state.deleting
    val tagError = state.tagError

    Column(
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.lg),
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            label = { Text(stringResource(R.string.field_name)) },
            isError = state.nameError,
            supportingText = if (state.nameError) {
                { Text(stringResource(R.string.field_name_error)) }
            } else {
                null
            },
            singleLine = true,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.notes,
            onValueChange = viewModel::setNotes,
            label = { Text(stringResource(R.string.field_notes)) },
            minLines = 2,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel(stringResource(R.string.field_type))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            TaskType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = state.type == type,
                    onClick = { viewModel.setType(type) },
                    enabled = !state.isEdit && !busy,
                    shape = SegmentedButtonDefaults.itemShape(index, TaskType.entries.size),
                ) {
                    SegmentText(typeLabel(type))
                }
            }
        }
        if (state.isEdit) {
            Text(
                text = stringResource(R.string.edit_type_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionLabel(stringResource(R.string.field_difficulty))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            Difficulty.entries.forEachIndexed { index, difficulty ->
                SegmentedButton(
                    selected = state.difficulty == difficulty,
                    onClick = { viewModel.setDifficulty(difficulty) },
                    enabled = !busy,
                    shape = SegmentedButtonDefaults.itemShape(index, Difficulty.entries.size),
                ) {
                    SegmentText(difficultyLabel(difficulty))
                }
            }
        }

        SectionLabel(stringResource(R.string.field_tags))
        Text(
            text = stringResource(R.string.tags_hint),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TagChips(
            available = state.availableTags,
            selected = state.selectedTags,
            enabled = !busy,
            onToggle = viewModel::toggleTag,
        )
        OutlinedTextField(
            value = state.tagDraft,
            onValueChange = viewModel::setTagDraft,
            label = { Text(stringResource(R.string.tag_new)) },
            placeholder = { Text(stringResource(R.string.tag_new_hint)) },
            singleLine = true,
            isError = tagError != null,
            supportingText = if (tagError != null) {
                { Text(tagErrorText(tagError)) }
            } else {
                null
            },
            trailingIcon = {
                IconButton(
                    onClick = viewModel::addTag,
                    enabled = !busy && state.tagDraft.isNotBlank(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.tag_add),
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.addTag() }),
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.tags_limit_hint, TagRules.MAX_TAGS),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagChips(
    available: List<Tag>,
    selected: List<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit,
) {
    val items = remember(available, selected) { buildTagChips(available, selected) }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
        verticalArrangement = Arrangement.spacedBy(Dimens.sm),
    ) {
        items.forEach { item ->
            FilterChip(
                selected = item.selected,
                onClick = { onToggle(item.name) },
                enabled = enabled,
                label = { Text(item.name) },
                leadingIcon = { TagDot(color = item.color) },
                trailingIcon = if (item.selected) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else {
                    null
                },
            )
        }
    }
}

private data class TagChipItem(val name: String, val color: String?, val selected: Boolean)

private fun buildTagChips(available: List<Tag>, selected: List<String>): List<TagChipItem> {
    val selectedKeys = selected.map { it.lowercase(Locale.ROOT) }.toSet()
    val availableKeys = available.map { it.name.lowercase(Locale.ROOT) }.toSet()
    val known = available.map { tag ->
        TagChipItem(
            name = tag.name,
            color = tag.color,
            selected = tag.name.lowercase(Locale.ROOT) in selectedKeys,
        )
    }
    // Только что введённые теги ещё не в БД — показываем их отдельными чипами.
    val fresh = selected
        .filter { it.lowercase(Locale.ROOT) !in availableKeys }
        .map { name -> TagChipItem(name = name, color = null, selected = true) }
    return known + fresh
}

@Composable
private fun FormLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.md),
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.loading_task),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FormLoadFailed(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.md),
            modifier = Modifier.padding(Dimens.screenPadding),
        ) {
            Text(
                text = stringResource(R.string.task_not_found),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.task_not_found_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onBack) {
                Text(stringResource(R.string.back_to_list))
            }
        }
    }
}

/** Подпись сегмента: не даём длинному слову разъехаться на узком экране. */
@Composable
private fun SegmentText(text: String) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun tagErrorText(error: TagRules.Error): String = when (error) {
    TagRules.Error.EMPTY -> stringResource(R.string.tag_empty)
    TagRules.Error.TOO_LONG -> stringResource(R.string.tag_too_long, TagRules.MAX_NAME_LENGTH)
    TagRules.Error.LIMIT -> stringResource(R.string.tag_limit, TagRules.MAX_TAGS)
    TagRules.Error.DUPLICATE -> stringResource(R.string.tag_duplicate)
}

@Composable
private fun typeLabel(type: TaskType): String = stringResource(
    when (type) {
        TaskType.HABIT -> R.string.type_habit
        TaskType.DAILY -> R.string.type_daily
        TaskType.TODO -> R.string.type_todo
    },
)

@Composable
private fun difficultyLabel(difficulty: Difficulty): String = stringResource(
    when (difficulty) {
        Difficulty.TRIVIAL -> R.string.difficulty_trivial
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.MEDIUM -> R.string.difficulty_medium
        Difficulty.HARD -> R.string.difficulty_hard
    },
)
