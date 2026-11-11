package com.example.habittracker.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import com.example.habittracker.R
import com.example.habittracker.ui.theme.Dimens

/**
 * S06/S08: подтверждение удаления. Фокус по умолчанию — на «Отмена»,
 * чтобы случайный Enter не удалил задачу.
 */
@Composable
fun ConfirmDeleteDialog(
    taskName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cancelFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { cancelFocus.requestFocus() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(Dimens.iconMd),
            )
        },
        title = { Text(stringResource(R.string.delete_task_title)) },
        text = { Text(stringResource(R.string.delete_task_body, taskName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.focusRequester(cancelFocus),
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
