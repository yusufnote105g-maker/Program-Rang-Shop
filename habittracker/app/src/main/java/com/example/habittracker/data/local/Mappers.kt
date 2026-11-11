package com.example.habittracker.data.local

import com.example.habittracker.data.local.entity.TaskEntity
import com.example.habittracker.data.local.entity.TagEntity
import com.example.habittracker.data.local.entity.UserEntity
import com.example.habittracker.domain.cron.CronEngine
import com.example.habittracker.domain.cron.TaskSchedule
import com.example.habittracker.domain.model.Difficulty
import com.example.habittracker.domain.model.Frequency
import com.example.habittracker.domain.model.Tag
import com.example.habittracker.domain.model.Task
import com.example.habittracker.domain.model.TaskType
import com.example.habittracker.domain.model.UserStats
import com.example.habittracker.domain.reward.RewardEngine
import java.time.LocalDate

/**
 * @param today локальный день пользователя. Если передан, задача знает,
 *   нужна ли она сегодня (`dueToday`) — это нужно для серых чекбоксов
 *   у ежедневных с расписанием.
 * @param tags теги задачи; подтягиваются отдельным join-запросом.
 */
fun TaskEntity.toDomain(today: LocalDate? = null, tags: List<Tag> = emptyList()): Task = Task(
    id = id,
    type = enumOrDefault(type, TaskType.HABIT),
    text = text,
    notes = notes,
    difficulty = enumOrDefault(difficulty, Difficulty.EASY),
    up = up,
    down = down,
    counterUp = counterUp,
    counterDown = counterDown,
    streak = streak,
    bestStreak = bestStreak,
    frequency = enumOrDefault(frequency, Frequency.DAILY),
    repeatDays = repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() },
    lastCompletedDay = lastCompletedDay,
    dueToday = today == null || CronEngine.isDue(toSchedule(), today),
    completed = completed,
    position = position,
    archived = archived,
    tags = tags,
)

fun TagEntity.toDomain(): Tag = Tag(id = id, name = name, color = color)

/** Расписание для календарной логики (CronEngine). */
fun TaskEntity.toSchedule(): TaskSchedule = TaskSchedule(
    startDate = runCatching { LocalDate.parse(startDate) }.getOrDefault(LocalDate.of(1970, 1, 1)),
    frequency = enumOrDefault(frequency, Frequency.DAILY),
    everyX = everyX,
    repeatDays = repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet(),
)

fun UserEntity.toStats(): UserStats = UserStats(
    level = level,
    xp = xp,
    xpToNext = RewardEngine.xpToNext(level),
    hp = hp,
    maxHp = maxHp,
    gold = gold,
    gems = gems,
)

private inline fun <reified T : Enum<T>> enumOrDefault(name: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: fallback
