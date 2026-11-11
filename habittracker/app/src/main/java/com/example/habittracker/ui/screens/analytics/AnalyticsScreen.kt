package com.example.habittracker.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.habittracker.R
import com.example.habittracker.domain.model.AnalyticsData
import com.example.habittracker.domain.model.DailyActivity
import com.example.habittracker.domain.model.TaskType
import com.example.habittracker.ui.theme.Dimens
import com.example.habittracker.ui.theme.extra

/** S09: аналитика прогресса. Числа считаются из task_logs, не выдумываются. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.analytics_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error -> AnalyticsEmpty(onBack = onBack)
                state.data == null || state.data?.hasHistory != true -> AnalyticsEmpty(onBack = onBack)
                else -> AnalyticsContent(data = state.data!!)
            }
        }
    }
}

@Composable
private fun AnalyticsContent(data: AnalyticsData) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.lg),
    ) {
        item {
            Text(
                text = stringResource(R.string.analytics_range, data.rangeDays),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { SummaryGrid(data = data) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                Text(
                    text = stringResource(R.string.analytics_chart_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                val days = data.daily.takeLast(CHART_DAYS)
                val chartA11y = stringResource(
                    R.string.analytics_chart_a11y,
                    days.size,
                    days.joinToString(", ") { "${it.day.dayOfMonth}:${it.completions}" },
                )
                ActivityChart(
                    days = days,
                    modifier = Modifier.semantics { contentDescription = chartA11y },
                )
                Text(
                    text = stringResource(R.string.analytics_range, CHART_DAYS),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Text(
                text = stringResource(R.string.analytics_streaks_title),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (data.topStreaks.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.analytics_no_streaks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(data.topStreaks, key = { it.id }) { streak ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(
                            R.string.analytics_streak_item,
                            streak.text,
                            streak.current,
                            streak.best,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(Dimens.md),
                    )
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.analytics_types_title),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        items(data.counts, key = { it.type.name }) { count ->
            Text(
                text = stringResource(
                    R.string.analytics_type_item,
                    typeLabel(count.type),
                    count.total,
                    count.completed,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Text(
                text = context.getString(
                    R.string.analytics_range,
                    data.rangeDays,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SummaryGrid(data: AnalyticsData) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
            StatCard(
                value = data.totalCompletions.toString(),
                label = stringResource(R.string.analytics_completions),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = data.totalMisses.toString(),
                label = stringResource(R.string.analytics_misses),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
            StatCard(
                value = "+${data.xpEarned}",
                label = stringResource(R.string.analytics_xp),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = "+${data.goldEarned}",
                label = stringResource(R.string.analytics_gold),
                modifier = Modifier.weight(1f),
                accentGold = true,
            )
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accentGold: Boolean = false,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(Dimens.md)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (accentGold) {
                    MaterialTheme.extra.gold
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActivityChart(
    days: List<DailyActivity>,
    modifier: Modifier = Modifier,
) {
    if (days.isEmpty()) return
    val max = days.maxOf { it.completions }.coerceAtLeast(1)
    val barArea = 96.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(124.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { day ->
            val height = (barArea.value * day.completions / max)
                .coerceIn(2f, barArea.value)
                .dp
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(height)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                        ),
                )
                Spacer(Modifier.height(Dimens.xs))
                Text(
                    text = day.day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
private fun AnalyticsEmpty(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.analytics_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(Dimens.sm))
        Text(
            text = stringResource(R.string.analytics_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(Dimens.xl))
        Button(onClick = onBack) {
            Text(stringResource(R.string.analytics_empty_cta))
        }
    }
}

@Composable
private fun typeLabel(type: TaskType): String = stringResource(
    when (type) {
        TaskType.HABIT -> R.string.type_habit
        TaskType.DAILY -> R.string.type_daily
        TaskType.TODO -> R.string.type_todo
    },
)

private const val CHART_DAYS = 14
