package com.example.habittracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.example.habittracker.R
import com.example.habittracker.domain.model.UserStats
import com.example.habittracker.ui.theme.Dimens
import com.example.habittracker.ui.theme.extra

/** Шапка игрока (HP/XP/уровень/золото). Спека: components.md -> AvatarHeader. */
@Composable
fun AvatarHeader(stats: UserStats?, modifier: Modifier = Modifier) {
    if (stats == null) return
    val lowHp = stats.hp <= stats.maxHp / 4
    val a11y = stringResource(
        R.string.stats_a11y,
        stats.level, stats.xp, stats.xpToNext, stats.hp, stats.maxHp, stats.gold,
    )
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = a11y },
    ) {
        Column(modifier = Modifier.padding(Dimens.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.level_short, stats.level),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.gold_short, stats.gold),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.extra.gold,
                )
            }
            Spacer(Modifier.height(Dimens.sm))
            // Подпись обязательна: прогресс нельзя передавать только цветом полосы.
            Text(
                text = stringResource(R.string.xp_label, stats.xp, stats.xpToNext),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Dimens.xs))
            LinearProgressIndicator(
                progress = { stats.xp.toFloat() / stats.xpToNext.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(Dimens.sm))
            Text(
                text = stringResource(R.string.hp_label, stats.hp, stats.maxHp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Dimens.xs))
            LinearProgressIndicator(
                progress = { stats.hp.toFloat() / stats.maxHp.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth(),
                color = if (lowHp) MaterialTheme.colorScheme.error else MaterialTheme.extra.success,
            )
        }
    }
}
