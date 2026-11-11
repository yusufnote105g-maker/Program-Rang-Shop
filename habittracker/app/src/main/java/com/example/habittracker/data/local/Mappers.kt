package com.example.habittracker.data.local

import com.example.habittracker.data.local.entity.TaskEntity
import com.example.habittracker.data.local.entity.UserEntity
import com.example.habittracker.domain.cron.TaskSchedule
import com.example.habittracker.domain.model.Difficulty
import com.example.habittracker.domain.model.Frequency
import com.example.habittracker.domain.model.Task
import com.example.habittracker.domain.model.TaskType
import com.example.habittracker.domain.model.UserStats
import com.example.habittracker.domain.reward.RewardEngine
import java.time.LocalDate

fun TaskEntity.toDomain(): Task = Task(
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
    completed = completed,
    position = position,
    archived = archived,
)

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
