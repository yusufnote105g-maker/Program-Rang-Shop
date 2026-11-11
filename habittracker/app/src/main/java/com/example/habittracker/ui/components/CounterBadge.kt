package com.example.habittracker.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.example.habittracker.R
import com.example.habittracker.ui.theme.Dimens

/**
 * Счётчик отметок привычки за сегодня: «+N» или «−M».
 * Спека: replica/design/components.md -> CounterBadge.
 * Показывается только когда есть что показать (count > 0).
 */
@Composable
fun CounterBadge(
    count: Int,
    plus: Boolean,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
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
    val a11y = stringResource(
        if (plus) R.string.counter_up_a11y else R.string.counter_down_a11y,
        count,
    )
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = container,
        modifier = modifier.clearAndSetSemantics { contentDescription = a11y },
    ) {
        Text(
            text = stringResource(
                if (plus) R.string.counter_up_value else R.string.counter_down_value,
                count,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(
                horizontal = Dimens.counterBadgePadH,
                vertical = Dimens.counterBadgePadV,
            ),
        )
    }
}
