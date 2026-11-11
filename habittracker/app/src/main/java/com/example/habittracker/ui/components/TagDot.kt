package com.example.habittracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.habittracker.ui.theme.Dimens

/** Цветная точка тега. Цвет приходит из tags.color (данные), не из UI-константы. */
@Composable
fun TagDot(color: String?, modifier: Modifier = Modifier) {
    val dotColor = color?.let { raw ->
        remember(raw) { parseTagColor(raw) }
    } ?: MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .size(Dimens.sm)
            .clip(CircleShape)
            .background(dotColor),
    )
}

private fun parseTagColor(raw: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(raw)) }
        .getOrDefault(Color.Gray)
