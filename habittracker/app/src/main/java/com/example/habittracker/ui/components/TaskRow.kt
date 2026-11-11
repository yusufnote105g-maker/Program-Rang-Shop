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
                    Checkbox(checked = doneToday, onCheckedChange = { if (it) onPlus() })
                    TaskLabel(task = task, modifier = Modifier.weight(1f), showStreak = true)
                }

                TaskType.TODO -> {
                    Checkbox(checked = task.completed, onCheckedChange = { if (it) onComplete() })
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
) {
    Column(modifier = modifier) {
        Text(
            text = task.text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (showStreak && task.streak > 0) {
            Text(
                text = stringResource(R.string.streak_label, task.streak),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (task.notes.isNotBlank()) {
            Text(
                text = task.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
