package com.example.habittracker.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.habittracker.ui.theme.Dimens

/** Кнопка «+» / «−» для привычки. Спека: replica/design/components.md -> CounterButton. */
@Composable
fun CounterButton(
    plus: Boolean,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (plus) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val content = if (plus) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(Dimens.touchTargetMin),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = container,
            contentColor = content,
        ),
    ) {
        Icon(
            imageVector = if (plus) Icons.Default.Add else Icons.Default.Remove,
            contentDescription = contentDescription,
            modifier = Modifier.size(Dimens.iconMd),
        )
    }
}
