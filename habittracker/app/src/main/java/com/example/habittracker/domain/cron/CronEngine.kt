package com.example.habittracker.domain.cron

import com.example.habittracker.domain.model.Frequency
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Расписание задачи — всё, что нужно, чтобы ответить «нужна она в этот день или нет».
 */
data class TaskSchedule(
    val startDate: LocalDate,
    val frequency: Frequency,
    val everyX: Int = 1,
    val repeatDays: Set<Int> = emptySet(),
)

/**
 * Календарная логика ежедневных задач. Чистые функции без Android и без БД,
 * поэтому её легко покрыть тестами.
 *
 * Дни недели: понедельник = 0 … воскресенье = 6 (ISO-значение минус один).
 */
object CronEngine {

    /** Сколько дней назад ищем «предыдущий нужный день» — защита от бесконечного цикла. */
    private const val SEARCH_LIMIT_DAYS = 400L

    /** Нужно ли выполнять задачу в конкретный день. */
    fun isDue(schedule: TaskSchedule, day: LocalDate): Boolean {
        if (day.isBefore(schedule.startDate)) return false
        return when (schedule.frequency) {
            Frequency.DAILY -> {
                val step = schedule.everyX.coerceAtLeast(1)
                ChronoUnit.DAYS.between(schedule.startDate, day) % step == 0L
            }

            Frequency.WEEKLY ->
                schedule.repeatDays.isEmpty() || isoDay(day) in schedule.repeatDays

            Frequency.MONTHLY -> {
                // 31-е в коротком месяце сдвигаем на последний день месяца.
                val target = schedule.startDate.dayOfMonth
                day.dayOfMonth == minOf(target, day.lengthOfMonth())
            }
        }
    }

    /** Последний день до [before], когда задача была нужна. null — если такого дня нет. */
    fun previousDueDay(schedule: TaskSchedule, before: LocalDate): LocalDate? {
        var day = before.minusDays(1)
        var guard = 0L
        while (guard < SEARCH_LIMIT_DAYS) {
            if (isDue(schedule, day)) return day
            day = day.minusDays(1)
            guard++
        }
        return null
    }

    /**
     * Новая серия после отметки. Продолжаем серию, только если предыдущий нужный день
     * был закрыт; пропуск обнуляет её, и счёт начинается заново с единицы.
     */
    fun streakAfterCompletion(
        currentStreak: Int,
        lastCompletedDay: LocalDate?,
        schedule: TaskSchedule,
        today: LocalDate,
    ): Int {
        val previous = previousDueDay(schedule, today) ?: return 1
        return if (lastCompletedDay == previous) currentStreak + 1 else 1
    }

    /** Понедельник = 0 … воскресенье = 6. */
    fun isoDay(day: LocalDate): Int = day.dayOfWeek.value - 1
}
