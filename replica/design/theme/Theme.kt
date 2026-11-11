package com.example.habittracker.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Маппинг tokens.json -> "material3" (роли M3 на наши семантические токены).
private val LightScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightOnAccent,
    primaryContainer = LightAccentContainer,
    onPrimaryContainer = LightOnAccentContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    background = LightBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextMuted,
    outline = LightBorderInput,
    outlineVariant = LightBorder,
    error = LightDanger,
    onError = LightOnDanger,
    errorContainer = LightDangerContainer,
    onErrorContainer = LightOnDangerContainer,
)

private val DarkScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkOnAccent,
    primaryContainer = DarkAccentContainer,
    onPrimaryContainer = DarkOnAccentContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextMuted,
    outline = DarkBorderInput,
    outlineVariant = DarkBorder,
    error = DarkDanger,
    onError = DarkOnDanger,
    errorContainer = DarkDangerContainer,
    onErrorContainer = DarkOnDangerContainer,
)

// Роли, которых нет в ColorScheme M3 (успех, предупреждение, игровое золото, границы).
data class ExtraColors(
    val success: Color,
    val warning: Color,
    val gold: Color,
    val border: Color,
    val borderInput: Color,
)

val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(
        success = LightSuccess,
        warning = LightWarning,
        gold = LightGold,
        border = LightBorder,
        borderInput = LightBorderInput,
    )
}

/**
 * Тема приложения. Динамический цвет (Android 12+) намеренно выключен:
 * бренд задаётся ролью accent, иначе система перекрасит приложение под обои.
 */
@Composable
fun HabitTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val extra = if (darkTheme) {
        ExtraColors(DarkSuccess, DarkWarning, DarkGold, DarkBorder, DarkBorderInput)
    } else {
        ExtraColors(LightSuccess, LightWarning, LightGold, LightBorder, LightBorderInput)
    }
    CompositionLocalProvider(LocalExtraColors provides extra) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

// Удобный доступ: MaterialTheme.extra.success и т.п.
val MaterialTheme.extra: ExtraColors
    @Composable get() = LocalExtraColors.current
