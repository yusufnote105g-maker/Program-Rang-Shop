package com.example.habittracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.habittracker.R
import com.example.habittracker.domain.model.Task
import com.example.habittracker.domain.model.TaskType
import com.example.habittracker.ui.theme.Dimens

/** Строка задачи. Спека: replica/design/components.md -> TaskRow. */
@Composable
fun TaskRow(
    task: Task,
    today: String,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = Dimens.taskRowMin)
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
