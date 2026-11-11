package com.example.habittracker.data.repository

import androidx.room.withTransaction
import com.example.habittracker.data.local.AppDatabase
import com.example.habittracker.data.local.dao.TaskDao
import com.example.habittracker.data.local.dao.TaskLogDao
import com.example.habittracker.data.local.dao.UserDao
import com.example.habittracker.data.local.entity.TaskEntity
import com.example.habittracker.data.local.entity.TaskLogEntity
import com.example.habittracker.data.local.entity.UserEntity
import com.example.habittracker.data.local.toDomain
import com.example.habittracker.data.local.toSchedule
import com.example.habittracker.data.local.toStats
import com.example.habittracker.domain.cron.CronEngine
import com.example.habittracker.domain.model.DailyResetReport
import com.example.habittracker.domain.model.Difficulty
import com.example.habittracker.domain.model.RewardResult
import com.example.habittracker.domain.model.Task
import com.example.habittracker.domain.model.TaskType
import com.example.habittracker.domain.model.UserStats
import com.example.habittracker.domain.reward.RewardEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Единая точка доступа к данным. Все изменения, затрагивающие и задачу, и игрока,
 * идут внутри одной транзакции — поэтому быстрые тапы и фоновый сброс не теряют данные.
 */
@Singleton
class TaskRepository @Inject constructor(
    private val db: AppDatabase,
    private val userDao: UserDao,
    private val taskDao: TaskDao,
    private val taskLogDao: TaskLogDao,
    private val clock: Clock,
) {

    fun observeUser(): Flow<UserStats?> = userDao.observeUser().map { it?.toStats() }

    /** Реактивный «сегодняшний» ключ дня — с учётом зоны и границы дня пользователя. */
    fun observeToday(): Flow<String> =
        userDao.observeUser().map { dayKey(it, clock.instant()).toString() }

    fun observeTasks(type: TaskType): Flow<List<Task>> =
        taskDao.observeByType(type.name).map { list -> list.map { it.toDomain() } }

    /** Гарантирует, что игрок существует (вызывается при старте). */
    suspend fun ensureUser() {
        requireUser()
    }

    suspend fun createTask(
        type: TaskType,
        text: String,
        notes: String,
        difficulty: Difficulty,
        up: Boolean,
        down: Boolean,
    ): String = db.withTransaction {
        val user = requireUser()
        val now = clock.millis()
        val id = UUID.randomUUID().toString()
        taskDao.insert(
            TaskEntity(
                id = id,
                userId = user.id,
                type = type.name,
                text = text.trim(),
                notes = notes.trim(),
                difficulty = difficulty.name,
                up = up,
                down = down,
                startDate = dayKey(user, clock.instant()).toString(),
                position = taskDao.nextPosition(user.id),
                createdAt = now,
                updatedAt = now,
            ),
        )
        id
    }

    /** Положительная отметка. Возвращает null, если действие недопустимо (например, daily уже закрыт). */
    suspend fun tapPlus(taskId: String): RewardResult? = db.withTransaction {
        val user = requireUser()
        val task = taskDao.getById(taskId) ?: return@withTransaction null
        val now = clock.millis()
        val dayDate = dayKey(user, clock.instant())
        val day = dayDate.toString()

        if (task.type == TaskType.DAILY.name && task.lastCompletedDay == day) {
            return@withTransaction null
        }

        val diff = Difficulty.entries.firstOrNull { it.name == task.difficulty } ?: Difficulty.EASY
        val (xp, gold) = RewardEngine.rewardFor(diff)
        val applied = RewardEngine.applyXp(user.level, user.xp, xp)

        userDao.upsert(
            user.copy(
                level = applied.level,
                xp = applied.xp,
                gold = user.gold + gold,
                updatedAt = now,
            ),
        )

        val isDaily = task.type == TaskType.DAILY.name
        val newStreak = if (isDaily) {
            CronEngine.streakAfterCompletion(
                currentStreak = task.streak,
                lastCompletedDay = task.lastCompletedDay?.let { parseDay(it) },
                schedule = task.toSchedule(),
                today = dayDate,
            )
        } else {
            task.streak
        }
        taskDao.update(
            task.copy(
                counterUp = task.counterUp + 1,
                streak = newStreak,
                bestStreak = maxOf(task.bestStreak, newStreak),
                lastCompletedDay = if (isDaily) day else task.lastCompletedDay,
                updatedAt = now,
            ),
        )
        taskLogDao.insert(
            TaskLogEntity(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                userId = user.id,
                loggedAt = now,
                loggedDay = day,
                direction = "up",
                deltaXp = xp,
                deltaGold = gold,
                value = 1.0,
            ),
        )
        RewardResult(xpDelta = xp, goldDelta = gold, hpDelta = 0, leveledUp = applied.leveledUp, newLevel = applied.level)
    }

    /** Отрицательная отметка: списывает опыт и здоровье. */
    suspend fun tapMinus(taskId: String): RewardResult? = db.withTransaction {
        val user = requireUser()
        val task = taskDao.getById(taskId) ?: return@withTransaction null
        val now = clock.millis()
        val day = dayKey(user, clock.instant()).toString()

        val diff = Difficulty.entries.firstOrNull { it.name == task.difficulty } ?: Difficulty.EASY
        val (xpLoss, hpLoss) = RewardEngine.penaltyFor(diff)
        val applied = RewardEngine.applyXp(user.level, user.xp, -xpLoss)
        val newHp = RewardEngine.clampHp(user.hp - hpLoss, user.maxHp)

        userDao.upsert(
            user.copy(
                level = applied.level,
                xp = applied.xp,
                hp = newHp,
                updatedAt = now,
            ),
        )
        taskDao.update(task.copy(counterDown = task.counterDown + 1, updatedAt = now))
        taskLogDao.insert(
            TaskLogEntity(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                userId = user.id,
                loggedAt = now,
                loggedDay = day,
                direction = "down",
                deltaXp = -xpLoss,
                deltaGold = 0,
                value = 1.0,
            ),
        )
        RewardResult(xpDelta = -xpLoss, goldDelta = 0, hpDelta = -hpLoss, leveledUp = false, newLevel = applied.level)
    }

    /** Закрывает разовую задачу (todo). Возвращает null, если уже закрыта. */
    suspend fun completeTodo(taskId: String): RewardResult? = db.withTransaction {
        val user = requireUser()
        val task = taskDao.getById(taskId) ?: return@withTransaction null
        if (task.completed) return@withTransaction null
        val now = clock.millis()
        val day = dayKey(user, clock.instant()).toString()

        val diff = Difficulty.entries.firstOrNull { it.name == task.difficulty } ?: Difficulty.EASY
        val (xp, gold) = RewardEngine.rewardFor(diff)
        val applied = RewardEngine.applyXp(user.level, user.xp, xp)

        userDao.upsert(
            user.copy(level = applied.level, xp = applied.xp, gold = user.gold + gold, updatedAt = now),
        )
        taskDao.update(task.copy(completed = true, completedAt = now, updatedAt = now))
        taskLogDao.insert(
            TaskLogEntity(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                userId = user.id,
                loggedAt = now,
                loggedDay = day,
                direction = "complete",
                deltaXp = xp,
                deltaGold = gold,
                value = 1.0,
            ),
        )
        RewardResult(xp, gold, 0, applied.leveledUp, applied.level)
    }

    suspend fun deleteTask(taskId: String) = db.withTransaction {
        taskDao.deleteById(taskId)
    }

    /**
     * Ежедневный сброс (F06). Идемпотентен: если `last_cron_day` уже равен сегодняшнему
     * дню, ничего не делает и возвращает null.
     *
     * Что делает:
     *  1. проходит по каждому прошедшему дню, который ещё не «закрыт»;
     *  2. у ежедневных, которые были нужны, но не закрыты — обнуляет серию и списывает HP;
     *  3. обнуляет дневные счётчики привычек;
     *  4. ставит `last_cron_day` = сегодня.
     *
     * Если приложение не открывали неделю, дни обрабатываются по очереди — серия
     * всё равно обнулится, а штраф не превратится в один большой скачок.
     */
    suspend fun runDailyCron(): DailyResetReport? = db.withTransaction {
        val user = requireUser()
        val now = clock.millis()
        val today = dayKey(user, clock.instant())
        val last = user.lastCronDay?.let { parseDay(it) }

        if (last == null || !today.isAfter(last)) {
            if (user.lastCronDay != today.toString()) {
                userDao.upsert(user.copy(lastCronDay = today.toString(), updatedAt = now))
            }
            return@withTransaction null
        }

        // Не уходим в бесконечность, если данных нет годами: закрываем последние 400 дней.
        val days = generateSequence(last) { it.plusDays(1) }
            .takeWhile { it.isBefore(today) }
            .toList()
            .takeLast(MAX_SETTLED_DAYS)

        val dailies = taskDao.getDailies()
        val missedIds = LinkedHashSet<String>()
        var missed = 0
        var hpLost = 0

        for (day in days) {
            val key = day.toString()
            for (task in dailies) {
                if (!CronEngine.isDue(task.toSchedule(), day)) continue
                if (task.lastCompletedDay == key) continue // закрыта вовремя — серия уже начислена

                missed++
                missedIds += task.id
                val diff = Difficulty.entries.firstOrNull { it.name == task.difficulty } ?: Difficulty.EASY
                hpLost += diff.hpPenalty
                taskLogDao.insert(
                    TaskLogEntity(
                        id = UUID.randomUUID().toString(),
                        taskId = task.id,
                        userId = user.id,
                        loggedAt = now,
                        loggedDay = key,
                        direction = "miss",
                        deltaXp = 0,
                        deltaGold = 0,
                        value = 0.0,
                    ),
                )
            }
        }

        if (missedIds.isNotEmpty()) taskDao.resetStreaks(missedIds.toList(), now)
        taskDao.resetHabitCounters(now)

        val newHp = RewardEngine.clampHp(user.hp - hpLost, user.maxHp)
        userDao.upsert(user.copy(hp = newHp, lastCronDay = today.toString(), updatedAt = now))

        DailyResetReport(daysSettled = days.size, missedDailies = missed, hpLost = hpLost)
    }

    /** Сколько миллисекунд осталось до следующей границы дня пользователя. */
    suspend fun millisUntilNextDayBoundary(): Long {
        val user = requireUser()
        val zone = zoneOf(user)
        val now = clock.instant()
        val localNow = now.atZone(zone)
        var next = localNow.toLocalDate().atStartOfDay(zone)
            .plusMinutes(user.dayStartMinutes.toLong())
        if (!next.isAfter(localNow)) next = next.plusDays(1)
        return Duration.between(localNow, next).toMillis().coerceAtLeast(0)
    }

    // --- helpers ---

    private suspend fun requireUser(): UserEntity =
        userDao.getUser() ?: UserEntity(
            id = UUID.randomUUID().toString(),
            username = "Adventurer",
            timezone = TimeZone.getDefault().id,
        ).also { userDao.upsert(it) }

    /**
     * Ключ локального дня. Граница дня — местная полночь + `day_start_minutes`,
     * поэтому «сегодня» для игрока может ещё не наступить в 00:30.
     */
    private fun dayKey(user: UserEntity?, instant: Instant): LocalDate {
        val zone = user?.let { zoneOf(it) } ?: ZoneId.systemDefault()
        val startMinutes = user?.dayStartMinutes ?: 0
        return instant.atZone(zone).minusMinutes(startMinutes.toLong()).toLocalDate()
    }

    private fun zoneOf(user: UserEntity): ZoneId =
        runCatching { ZoneId.of(user.timezone) }.getOrDefault(ZoneId.systemDefault())

    private fun parseDay(value: String): LocalDate? =
        runCatching { LocalDate.parse(value) }.getOrNull()

    private companion object {
        const val MAX_SETTLED_DAYS = 400
    }
}
