package com.example.habittracker.ui.components

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.example.habittracker.R
import com.example.habittracker.domain.model.Task
import com.example.habittracker.domain.model.TaskType
import com.example.habittracker.ui.theme.Dimens

/**
 * S08: перенос между типами. Текущий тип не предлагаем, а в теле честно
 * предупреждаем, что счётчики и серия сбросятся.
 */
@Composable
fun MoveTaskDialog(
    task: Task,
    onConfirm: (TaskType) -> Unit,
    onDismiss: () -> Unit,
) {
    val targets = remember(task.type) { TaskType.entries.filter { it != task.type } }
    var selected by remember(task.id) { mutableStateOf(targets.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.move_task_title)) },
        text = {
            Column {
                Text(stringResource(R.string.move_task_body, task.text))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.md)
                        .selectableGroup(),
                ) {
                    targets.forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selected == type,
                                    onClick = { selected = type },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = Dimens.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selected == type,
                                onClick = null,
                            )
                            Text(
                                text = moveTypeLabel(type),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = Dimens.md),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selected?.let(onConfirm) },
                enabled = selected != null,
            ) {
                Text(stringResource(R.string.move_task_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun moveTypeLabel(type: TaskType): String = stringResource(
    when (type) {
        TaskType.HABIT -> R.string.move_to_habit
        TaskType.DAILY -> R.string.move_to_daily
        TaskType.TODO -> R.string.move_to_todo
    },
)
