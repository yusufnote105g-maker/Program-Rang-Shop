package com.example.habittracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.habittracker.R
import com.example.habittracker.domain.model.Tag
import com.example.habittracker.domain.model.Task
import com.example.habittracker.domain.model.TaskType
import com.example.habittracker.ui.theme.Dimens

/** Сколько цветных точек рисуем в строке; остальные видны в подписи тегов. */
private const val MAX_TAG_DOTS = 4

/** Строка задачи. Спека: replica/design/components.md -> TaskRow. */
@Composable
fun TaskRow(
    task: Task,
    today: String,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onMove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editLabel = stringResource(R.string.edit_task)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = Dimens.taskRowMin)
                .clickable(onClickLabel = editLabel, onClick = onEdit)
                .padding(horizontal = Dimens.screenPadding, vertical = Dimens.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
        ) {
            when (task.type) {
                TaskType.HABIT -> {
                    TaskLabel(task = task, modifier = Modifier.weight(1f))
                    CounterBadge(count = task.counterDown, plus = false)
                    CounterBadge(count = task.counterUp, plus = true)
                    CounterButton(
                        plus = false,
                        enabled = task.down,
                        contentDescription = stringResource(R.string.mark_done_minus, task.text),
                        onClick = onMinus,
                    )
                    CounterButton(
                        plus = true,
                        enabled = task.up,
                        contentDescription = stringResource(R.string.mark_done_plus, task.text),
                        onClick = onPlus,
                    )
                }

                TaskType.DAILY -> {
                    val doneToday = task.lastCompletedDay == today
                    val a11y = stringResource(R.string.mark_done_plus, task.text)
                    Checkbox(
                        checked = doneToday,
                        enabled = task.dueToday && !doneToday,
                        onCheckedChange = { if (it) onPlus() },
                        modifier = Modifier.semantics { contentDescription = a11y },
                    )
                    TaskLabel(
                        task = task,
                        modifier = Modifier.weight(1f),
                        showStreak = true,
                        notDue = !task.dueToday,
                    )
                }

                TaskType.TODO -> {
                    val a11y = stringResource(R.string.complete_todo_a11y, task.text)
                    Checkbox(
                        checked = task.completed,
                        enabled = !task.completed,
                        onCheckedChange = { if (it) onComplete() },
                        modifier = Modifier.semantics { contentDescription = a11y },
                    )
                    TaskLabel(task = task, modifier = Modifier.weight(1f))
                }
            }
            TaskActionsMenu(
                task = task,
                onEdit = onEdit,
                onDuplicate = onDuplicate,
                onMove = onMove,
                onDelete = onDelete,
            )
        }
    }
}

/** S08: «⋮» → Edit / Duplicate / Move / Delete. */
@Composable
private fun TaskActionsMenu(
    task: Task,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.task_actions_a11y, task.text),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit_task)) },
                onClick = {
                    open = false
                    onEdit()
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.duplicate)) },
                onClick = {
                    open = false
                    onDuplicate()
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.move)) },
                onClick = {
                    open = false
                    onMove()
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null)
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    open = false
                    onDelete()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
            )
        }
    }
}

@Composable
private fun TaskLabel(
    task: Task,
    modifier: Modifier = Modifier,
    showStreak: Boolean = false,
    notDue: Boolean = false,
) {
    Column(modifier = modifier) {
        Text(
            text = task.text,
            style = MaterialTheme.typography.titleMedium,
            color = if (notDue) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        when {
            notDue -> SubLabel(stringResource(R.string.not_due_today))
            showStreak && task.streak > 0 -> SubLabel(stringResource(R.string.streak_label, task.streak))
            task.notes.isNotBlank() -> SubLabel(task.notes, maxLines = 1)
        }
        if (task.tags.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.xs))
            TaskTags(tags = task.tags)
        }
    }
}

@Composable
private fun TaskTags(tags: List<Tag>) {
    val names = tags.joinToString(", ") { it.name }
    val a11y = stringResource(R.string.tags_a11y, names)
    Row(
        modifier = Modifier.semantics { contentDescription = a11y },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
    ) {
        tags.take(MAX_TAG_DOTS).forEach { tag -> TagDot(color = tag.color) }
        Text(
            text = tags.joinToString(" · ") { it.name },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
private fun SubLabel(text: String, maxLines: Int = 1) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
