package com.example.habittracker.data.repository

import androidx.room.withTransaction
import com.example.habittracker.data.local.AppDatabase
import com.example.habittracker.data.local.dao.TagDao
import com.example.habittracker.data.local.dao.TaskDao
import com.example.habittracker.data.local.dao.TaskLogDao
import com.example.habittracker.data.local.dao.UserDao
import com.example.habittracker.data.local.entity.TagEntity
import com.example.habittracker.data.local.entity.TaskEntity
import com.example.habittracker.data.local.entity.TaskLogEntity
import com.example.habittracker.data.local.entity.TaskTagEntity
import com.example.habittracker.data.local.entity.UserEntity
import com.example.habittracker.data.local.toDomain
import com.example.habittracker.data.local.toSchedule
import com.example.habittracker.data.local.toStats
import com.example.habittracker.domain.cron.CronEngine
import com.example.habittracker.domain.model.DailyResetReport
import com.example.habittracker.domain.model.Difficulty
import com.example.habittracker.domain.model.RewardResult
import com.example.habittracker.domain.model.Tag
import com.example.habittracker.domain.model.Task
import com.example.habittracker.domain.model.TaskType
import com.example.habittracker.domain.model.UserStats
import com.example.habittracker.domain.reward.RewardEngine
import com.example.habittracker.domain.tags.TagRules
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
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
    private val tagDao: TagDao,
    private val taskLogDao: TaskLogDao,
    private val clock: Clock,
) {

    fun observeUser(): Flow<UserStats?> = userDao.observeUser().map { it?.toStats() }

    /**
     * Реактивный «сегодняшний» ключ дня. Пересчитывается сам на границе дня,
     * поэтому открытое приложение не показывает вчерашний день до перезапуска.
     */
    fun observeToday(): Flow<String> = flow {
        while (true) {
            val user = userDao.getUser()
            val now = clock.instant()
            emit(dayKey(user, now).toString())
            delay(millisUntilBoundary(user, now).coerceAtLeast(MIN_TICK_MILLIS))
        }
    }

    fun observeTasks(type: TaskType): Flow<List<Task>> =
        combine(
            taskDao.observeByType(type.name),
            userDao.observeUser(),
            observeToday(),
            tagDao.observeTaskTags(),
        ) { list, user, todayKey, tagRows ->
            // Ключ дня приходит из тикера, поэтому dueToday пересчитывается
            // и на границе дня, а не только при изменении задачи/игрока.
            val today = runCatching { LocalDate.parse(todayKey) }.getOrNull()
                ?: dayKey(user, clock.instant())
            val tagsByTask = tagRows.groupBy(
                keySelector = { it.taskId },
                valueTransform = { Tag(id = it.tagId, name = it.name, color = it.color) },
            )
            list.map { it.toDomain(today, tagsByTask[it.id].orEmpty()) }
        }

    fun observeTags(): Flow<List<Tag>> =
        tagDao.observeTags().map { list -> list.map { it.toDomain() } }

    /**
     * Гарантирует, что игрок существует, и держит его часовой пояс в актуальном
     * состоянии: если устройство переехало в другую зону, граница дня сдвинется.
     */
    suspend fun ensureUser() {
        val user = requireUser()
        val systemZone = TimeZone.getDefault().id
        if (user.timezone != systemZone) {
            userDao.upsert(user.copy(timezone = systemZone, updatedAt = clock.millis()))
        }
    }

    suspend fun createTask(
        type: TaskType,
        text: String,
        notes: String,
        difficulty: Difficulty,
        up: Boolean,
        down: Boolean,
        tags: List<String> = emptyList(),
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
        replaceTags(user.id, id, tags)
        id
    }

    /** Загружает задачу для S06 вместе с тегами. */
    suspend fun loadTask(taskId: String): Task? {
        val task = taskDao.getById(taskId) ?: return null
        val user = userDao.getUser()
        val tags = tagDao.getTagsForTask(taskId).map { it.toDomain() }
        return task.toDomain(dayKey(user, clock.instant()), tags)
    }

    /**
     * S06: меняем только редактируемые поля. Тип, серия, счётчики и история
     * остаются как есть — смена типа сломала бы инварианты БД и streak.
     */
    suspend fun updateTask(
        taskId: String,
        text: String,
        notes: String,
        difficulty: Difficulty,
        tags: List<String>,
    ): Boolean = db.withTransaction {
        val task = taskDao.getById(taskId) ?: return@withTransaction false
        taskDao.update(
            task.copy(
                text = text.trim(),
                notes = notes.trim(),
                difficulty = difficulty.name,
                updatedAt = clock.millis(),
            ),
        )
        replaceTags(task.userId, taskId, tags)
        true
    }

    /**
     * Положительная отметка. Возвращает null, если действие недопустимо:
     * привычка без «+», ежедневная не по расписанию или уже закрытая сегодня.
     */
    suspend fun tapPlus(taskId: String): RewardResult? = db.withTransaction {
        val user = requireUser()
        val task = taskDao.getById(taskId) ?: return@withTransaction null
        val now = clock.millis()
        val dayDate = dayKey(user, clock.instant())
        val day = dayDate.toString()
        val schedule = task.toSchedule()

        when (task.type) {
            TaskType.HABIT.name -> if (!task.up) return@withTransaction null
            TaskType.DAILY.name -> {
                if (!task.up) return@withTransaction null
                if (!CronEngine.isDue(schedule, dayDate)) return@withTransaction null
                if (task.lastCompletedDay == day) return@withTransaction null
            }

            else -> return@withTransaction null
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
                schedule = schedule,
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

    /** Отрицательная отметка привычки: списывает опыт и здоровье. */
    suspend fun tapMinus(taskId: String): RewardResult? = db.withTransaction {
        val user = requireUser()
        val task = taskDao.getById(taskId) ?: return@withTransaction null
        if (task.type != TaskType.HABIT.name || !task.down) return@withTransaction null
        val now = clock.millis()
        val day = dayKey(user, clock.instant()).toString()

        val diff = Difficulty.entries.firstOrNull { it.name == task.difficulty } ?: Difficulty.EASY
        val (xpLoss, hpLoss) = RewardEngine.penaltyFor(diff)
        val applied = RewardEngine.applyXp(user.level, user.xp, -xpLoss)
        val newHp = RewardEngine.clampHp(user.hp - hpLoss, user.maxHp)
        // Отдаём фактическое изменение, а не номинальный штраф: на нуле
        // здоровье/опыт не могут уйти в минус, и snackbar не должен это обещать.
        val actualXpDelta = applied.xp - user.xp
        val actualHpDelta = newHp - user.hp

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
                deltaXp = actualXpDelta,
                deltaGold = 0,
                value = 1.0,
            ),
        )
        RewardResult(xpDelta = actualXpDelta, goldDelta = 0, hpDelta = actualHpDelta, leveledUp = false, newLevel = applied.level)
    }

    /** Закрывает разовую задачу (todo). Возвращает null, если уже закрыта. */
    suspend fun completeTodo(taskId: String): RewardResult? = db.withTransaction {
        val user = requireUser()
        val task = taskDao.getById(taskId) ?: return@withTransaction null
        if (task.type != TaskType.TODO.name || task.completed) return@withTransaction null
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

    /** Полностью заменяет набор тегов задачи. Новые теги создаются на лету. */
    private suspend fun replaceTags(userId: String, taskId: String, rawTags: List<String>) {
        tagDao.clearTaskTags(taskId)
        val now = clock.millis()
        TagRules.normalize(rawTags).forEach { name ->
            val tag = tagDao.findByName(userId, name) ?: TagEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                color = tagColor(name),
                createdAt = now,
                updatedAt = now,
            ).also { tagDao.upsert(it) }
            tagDao.insertTaskTag(TaskTagEntity(taskId = taskId, tagId = tag.id))
        }
    }

    /** Стабильный цвет из палитры токенов: один и тот же тег всегда одного цвета. */
    private fun tagColor(name: String): String =
        TAG_PALETTE[Math.floorMod(name.lowercase(Locale.ROOT).hashCode(), TAG_PALETTE.size)]

    /**
     * Ежедневный сброс (F06). Идемпотентен: если `last_cron_day` уже равен сегодняшнему
     * дню, ничего не делает и возвращает null.
     *
     * Что делает:
     *  1. проходит по каждому прошедшему дню, который ещё не «закрыт»;
     *  2. у ежедневных, которые были нужны, но не закрыты — списывает HP и обнуляет серию;
     *  3. обнуляет дневные счётчики привычек;
     *  4. ставит `last_cron_day` = сегодня.
     *
     * Серия НЕ обнуляется, если задача уже закрыта позже этого дня: тогда серию
     * пересчитала сама отметка, и обнуление стёрло бы только что заработанное.
     */
    suspend fun runDailyCron(): DailyResetReport? = db.withTransaction {
        val user = requireUser()
        val now = clock.millis()
        val today = dayKey(user, clock.instant())
        val last = user.lastCronDay?.let { parseDay(it) }

        if (last == null) {
            // Первый запуск: запоминаем сегодняшний день, но ничего не списываем.
            userDao.upsert(user.copy(lastCronDay = today.toString(), updatedAt = now))
            return@withTransaction null
        }
        // Часы могли перевести назад. Не откатываем last_cron_day — иначе
        // следующие запуски снова закроют уже закрытые дни и спишут HP повторно.
        if (!today.isAfter(last)) return@withTransaction null

        // Не уходим в бесконечность, если данных нет годами: закрываем последние 400 дней.
        val days = generateSequence(last) { it.plusDays(1) }
            .takeWhile { it.isBefore(today) }
            .toList()
            .takeLast(MAX_SETTLED_DAYS)

        val dailies = taskDao.getDailies()
        val streakToReset = LinkedHashSet<String>()
        var missed = 0
        var hpLost = 0

        for (day in days) {
            for (task in dailies) {
                if (!CronEngine.isDue(task.toSchedule(), day)) continue
                val lastDone = task.lastCompletedDay?.let { parseDay(it) }
                if (lastDone == day) continue // закрыта вовремя — серия уже начислена

                missed++
                val diff = Difficulty.entries.firstOrNull { it.name == task.difficulty } ?: Difficulty.EASY
                hpLost += diff.hpPenalty

                // Задача закрыта позже этого дня — серия уже пересчитана отметкой.
                val rebuiltLater = lastDone != null && lastDone.isAfter(day)
                if (!rebuiltLater) streakToReset += task.id

                taskLogDao.insert(
                    TaskLogEntity(
                        id = UUID.randomUUID().toString(),
                        taskId = task.id,
                        userId = user.id,
                        loggedAt = now,
                        loggedDay = day.toString(),
                        direction = "miss",
                        deltaXp = 0,
                        deltaGold = 0,
                        value = 0.0,
                    ),
                )
            }
        }

        if (streakToReset.isNotEmpty()) taskDao.resetStreaks(streakToReset.toList(), now)
        taskDao.resetHabitCounters(now)

        val newHp = RewardEngine.clampHp(user.hp - hpLost, user.maxHp)
        userDao.upsert(user.copy(hp = newHp, lastCronDay = today.toString(), updatedAt = now))

        DailyResetReport(
            daysSettled = days.size,
            missedDailies = missed,
            // Фактическая потеря: если HP уже 0, отчет не обещает минус.
            hpLost = (user.hp - newHp).coerceAtLeast(0),
        )
    }

    /** Сколько миллисекунд осталось до следующей границы дня пользователя. */
    suspend fun millisUntilNextDayBoundary(): Long {
        val user = requireUser()
        return millisUntilBoundary(user, clock.instant())
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

    private fun millisUntilBoundary(user: UserEntity?, now: Instant): Long {
        val zone = user?.let { zoneOf(it) } ?: ZoneId.systemDefault()
        val startMinutes = user?.dayStartMinutes ?: 0
        val localNow = now.atZone(zone)
        var next = localNow.toLocalDate().atStartOfDay(zone).plusMinutes(startMinutes.toLong())
        if (!next.isAfter(localNow)) next = next.plusDays(1)
        return Duration.between(localNow, next).toMillis().coerceAtLeast(0)
    }

    private fun zoneOf(user: UserEntity): ZoneId =
        runCatching { ZoneId.of(user.timezone) }.getOrDefault(ZoneId.systemDefault())

    private fun parseDay(value: String): LocalDate? =
        runCatching { LocalDate.parse(value) }.getOrNull()

    private companion object {
        const val MAX_SETTLED_DAYS = 400

        /** Минимальная пауза тикера дня — защита от нулевой задержки на границе. */
        const val MIN_TICK_MILLIS = 1_000L

        /** Цвета из replica/design/tokens.json; цвет тега — данные, не UI-стиль. */
        val TAG_PALETTE = listOf(
            "#3A4ED8", // accent
            "#1E7A4D", // success
            "#8A5A00", // warning
            "#4E5A6B", // secondary
            "#B3261E", // danger
            "#8A6A00", // gold
        )
    }
}
