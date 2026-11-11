package com.example.habittracker.domain.reminder

import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * Чистая арифметика напоминаний. Время хранится как epoch millis, но
 * ближайший запуск всегда пересчитывается в зоне пользователя — так
 * перевод часов и DST не сдвигают напоминание на сутки.
 */
object ReminderTime {

    const val DEFAULT_HOUR = 9
    const val DEFAULT_MINUTE = 0

    /** Ближайшее наступление [hour]:[minute] строго после [nowMillis]. */
    fun nextTrigger(
        nowMillis: Long,
        hour: Int,
        minute: Int,
        zone: ZoneId,
    ): Long {
        require(hour in 0..23) { "hour must be 0..23" }
        require(minute in 0..59) { "minute must be 0..59" }
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        var candidate = now.toLocalDate().atTime(hour, minute).atZone(zone)
        if (!candidate.toInstant().isAfter(now.toInstant())) {
            candidate = candidate.plusDays(1)
        }
        return candidate.toInstant().toEpochMilli()
    }

    /**
     * Следующее срабатывание после уже сохранённого. Добавляем дни к
     * локальному времени, а не 24 часа к timestamp: иначе на переходе
     * на летнее время напоминание уехало бы на час.
     */
    fun nextAfter(previousMillis: Long, nowMillis: Long, zone: ZoneId): Long {
        var candidate = Instant.ofEpochMilli(previousMillis).atZone(zone)
        val now = Instant.ofEpochMilli(nowMillis)
        while (!candidate.toInstant().isAfter(now)) {
            candidate = candidate.plusDays(1)
        }
        return candidate.toInstant().toEpochMilli()
    }

    fun hourOf(millis: Long?, zone: ZoneId): Int =
        millis?.let { Instant.ofEpochMilli(it).atZone(zone).hour } ?: DEFAULT_HOUR

    fun minuteOf(millis: Long?, zone: ZoneId): Int =
        millis?.let { Instant.ofEpochMilli(it).atZone(zone).minute } ?: DEFAULT_MINUTE

    /** Для тестов и UI: «09:05» без зависимости от локали. */
    fun format(hour: Int, minute: Int): String =
        String.format(Locale.ROOT, "%02d:%02d", hour, minute)
}
