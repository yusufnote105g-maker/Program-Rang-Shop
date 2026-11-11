package com.example.habittracker.domain.model

/** Тип задачи (S01/S02). */
enum class TaskType { HABIT, DAILY, TODO }

/**
 * Сложность влияет на награду и штраф (S02, S05).
 * Значения — наша математика, не значения оригинала.
 */
enum class Difficulty(val xp: Int, val gold: Int, val hpPenalty: Int) {
    TRIVIAL(1, 1, 1),
    EASY(2, 2, 2),
    MEDIUM(4, 3, 3),
    HARD(6, 5, 4),
}

enum class Frequency { DAILY, WEEKLY, MONTHLY }

/** Тег задачи (S02/S06/S01). Цвет хранится строкой #RRGGBB. */
data class Tag(
    val id: String,
    val name: String,
    val color: String = "#888888",
)

/** Доменная модель задачи (не зависит от Room). */
data class Task(
    val id: String,
    val type: TaskType,
    val text: String,
    val notes: String = "",
    val difficulty: Difficulty = Difficulty.EASY,
    val up: Boolean = true,
    val down: Boolean = false,
    val counterUp: Int = 0,
    val counterDown: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val frequency: Frequency = Frequency.DAILY,
    val repeatDays: List<Int> = emptyList(),
    /** Для daily: последний локальный день, когда задача была закрыта. */
    val lastCompletedDay: String? = null,
    /** Для daily: нужна ли задача именно сегодня (выходной по расписанию). */
    val dueToday: Boolean = true,
    val completed: Boolean = false,
    val position: Int = 0,
    val archived: Boolean = false,
    /** Напоминание включено; [reminderAt] — ближайший запуск в epoch millis. */
    val reminderEnabled: Boolean = false,
    val reminderAt: Long? = null,
    val tags: List<Tag> = emptyList(),
)

/** Итог отметки — для всплывающего фидбека (S05). */
data class RewardResult(
    val xpDelta: Int,
    val goldDelta: Int,
    val hpDelta: Int,
    val leveledUp: Boolean,
    val newLevel: Int,
)

/** Итог ежедневного сброса (F06). */
data class DailyResetReport(
    /** Сколько прошедших дней удалось закрыть (0 — сброс уже был сделан сегодня). */
    val daysSettled: Int,
    /** Сколько ежедневных задач было пропущено. */
    val missedDailies: Int,
    /** Сколько здоровья списано за пропуски. */
    val hpLost: Int,
)

/** Снимок игрока для шапки S01. */
data class UserStats(
    val level: Int,
    val xp: Int,
    val xpToNext: Int,
    val hp: Int,
    val maxHp: Int,
    val gold: Int,
    val gems: Int,
)
