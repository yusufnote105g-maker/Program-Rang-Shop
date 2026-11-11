package com.example.habittracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import com.example.habittracker.R
import com.example.habittracker.ui.theme.Dimens

/**
 * Скелетон списка на время первой загрузки. Рисуем форму строк, а не спиннер:
 * так экран не «прыгает», когда приходят данные.
 */
@Composable
fun TaskListSkeleton(
    modifier: Modifier = Modifier,
    rows: Int = 3,
) {
    val label = stringResource(R.string.loading_tasks)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.screenPadding)
            .semantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(Dimens.rowGap),
    ) {
        repeat(rows) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.taskRowMin),
            ) {}
        }
    }
}
