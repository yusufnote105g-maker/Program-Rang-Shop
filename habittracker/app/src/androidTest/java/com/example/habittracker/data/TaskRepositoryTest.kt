package com.example.habittracker.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.habittracker.data.local.AppDatabase
import com.example.habittracker.data.local.entity.TaskEntity
import com.example.habittracker.data.repository.TaskRepository
import com.example.habittracker.domain.model.Difficulty
import com.example.habittracker.domain.model.TaskType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Инструментальные тесты слоя данных на настоящей Room (in-memory).
 * Часы зафиксированы, поэтому «сегодня» в тестах — 2026-11-10 (зона Europe/Moscow).
 */
@RunWith(AndroidJUnit4::class)
class TaskRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: TaskRepository

    private val zone = ZoneId.of("Europe/Moscow")
    private val clock = Clock.fixed(Instant.parse("2026-11-10T09:00:00Z"), zone)
    private val today: LocalDate = LocalDate.of(2026, 11, 10)

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TaskRepository(db, db.userDao(), db.taskDao(), db.tagDao(), db.taskLogDao(), clock)

        // Игрок создаётся один раз; зону фиксируем, чтобы «сегодня» не зависело от телефона.
        repository.ensureUser()
        val user = db.userDao().getUser()!!
        db.userDao().upsert(user.copy(timezone = zone.id, lastCronDay = null))
    }

    @After
    fun tearDown() {
        db.close()
    }

    // --- создание и список (F01 / S01) ---

    @Test
    fun createHabit_appearsInListWithStartDateToday() = runBlocking {
        repository.createTask(
            type = TaskType.HABIT,
            text = "  Пить воду  ",
            notes = "",
            difficulty = Difficulty.EASY,
            up = true,
            down = true,
        )

        val tasks = repository.observeTasks(TaskType.HABIT).first()
        assertEquals(1, tasks.size)
        assertEquals("Пить воду", tasks.single().text)
        assertTrue(tasks.single().dueToday)
    }

    // --- отметки привычки (F02 / F03 / S04) ---

    @Test
    fun createTask_withTags_storesThemAndReusesNamesIgnoringCase() = runBlocking {
        val first = repository.createTask(
            type = TaskType.HABIT,
            text = "Бег",
            notes = "",
            difficulty = Difficulty.EASY,
            up = true,
            down = true,
            tags = listOf("здоровье", "утро"),
        )
        val second = repository.createTask(
            type = TaskType.HABIT,
            text = "Растяжка",
            notes = "",
            difficulty = Difficulty.EASY,
            up = true,
            down = true,
            tags = listOf("Здоровье"),
        )

        val tasks = repository.observeTasks(TaskType.HABIT).first()
        assertEquals(
            setOf("здоровье", "утро"),
            tasks.first { it.id == first }.tags.map { it.name }.toSet(),
        )
        assertEquals(
            setOf("здоровье"),
            tasks.first { it.id == second }.tags.map { it.name }.toSet(),
        )
        assertEquals(
            "«Здоровье» и «здоровье» — один тег",
            2,
            db.tagDao().observeTags().first().size,
        )
    }

    @Test
    fun updateTask_changesFieldsAndReplacesTags_butKeepsHistory() = runBlocking {
        val id = repository.createTask(
            type = TaskType.HABIT,
            text = "Старое имя",
            notes = "",
            difficulty = Difficulty.EASY,
            up = true,
            down = true,
            tags = listOf("дом"),
        )
        repository.tapPlus(id)

        val updated = repository.updateTask(
            taskId = id,
            text = "  Новое имя  ",
            notes = "  заметка  ",
            difficulty = Difficulty.HARD,
            tags = listOf("работа", "утро"),
        )

        assertTrue(updated)
        val task = repository.loadTask(id)!!
        assertEquals("Новое имя", task.text)
        assertEquals("заметка", task.notes)
        assertEquals(Difficulty.HARD, task.difficulty)
        assertEquals(setOf("работа", "утро"), task.tags.map { it.name }.toSet())
        assertEquals("История отметок не должна пропадать", 1, db.taskLogDao().countAll())
    }

    @Test
    fun updateTask_returnsFalse_whenTaskIsGone() = runBlocking {
        assertFalse(
            repository.updateTask("missing", "Имя", "", Difficulty.EASY, emptyList()),
        )
    }

    @Test
    fun deleteTask_removesLogsAndTagLinks_butKeepsTags() = runBlocking {
        val id = repository.createTask(
            type = TaskType.HABIT,
            text = "Удаляемая",
            notes = "",
            difficulty = Difficulty.EASY,
            up = true,
            down = true,
            tags = listOf("спорт"),
        )
        repository.tapPlus(id)

        repository.deleteTask(id)

        assertNull(db.taskDao().getById(id))
        assertEquals("Логи удаляются каскадом", 0, db.taskLogDao().countAll())
        assertEquals("Связи с тегами удаляются каскадом", 0, db.tagDao().countTaskTags(id))
        assertEquals("Сам тег остаётся для других задач", 1, db.tagDao().observeTags().first().size)
    }

    // --- отметки привычки (F02 / F03 / S04) ---

    @Test
    fun tapPlus_habit_countsEveryTapAndPaysGold() = runBlocking {
        val id = repository.createTask(TaskType.HABIT, "Зарядка", "", Difficulty.EASY, up = true, down = true)

        assertNotNull(repository.tapPlus(id))
        assertNotNull(repository.tapPlus(id))

        val task = db.taskDao().getById(id)!!
        assertEquals(2, task.counterUp)
        val user = db.userDao().getUser()!!
        assertEquals(2 * Difficulty.EASY.gold, user.gold)
    }

    @Test
    fun tapMinus_habit_losesHpAndCounts() = runBlocking {
        val id = repository.createTask(TaskType.HABIT, "Сладкое", "", Difficulty.EASY, up = true, down = true)

        val result = repository.tapMinus(id)

        assertNotNull(result)
        assertEquals(-Difficulty.EASY.hpPenalty, result!!.hpDelta)
        val user = db.userDao().getUser()!!
        assertEquals(50 - Difficulty.EASY.hpPenalty, user.hp)
        assertEquals(1, db.taskDao().getById(id)!!.counterDown)
    }

    @Test
    fun tapMinus_isRejected_whenMinusIsDisabled() = runBlocking {
        val id = repository.createTask(TaskType.HABIT, "Только плюс", "", Difficulty.EASY, up = true, down = false)
        assertNull(repository.tapMinus(id))
    }

    @Test
    fun tapMinus_reportsActualDelta_whenHpAndXpAreAlreadyZero() = runBlocking {
        val id = repository.createTask(TaskType.HABIT, "Сладкое", "", Difficulty.EASY, up = true, down = true)
        val user = db.userDao().getUser()!!
        db.userDao().upsert(user.copy(hp = 0, xp = 0))

        val result = repository.tapMinus(id)!!

        assertEquals("HP не может уйти ниже нуля", 0, result.hpDelta)
        assertEquals("Опыт не может уйти ниже нуля", 0, result.xpDelta)
    }

    // --- ежедневные (F02 / F06) ---

    @Test
    fun tapPlus_daily_isRejectedTwiceOnTheSameDay() = runBlocking {
        val id = repository.createTask(TaskType.DAILY, "Чтение", "", Difficulty.EASY, up = true, down = false)

        assertNotNull(repository.tapPlus(id))
        assertNull("Повторная отметка за день должна отклоняться", repository.tapPlus(id))

        val task = db.taskDao().getById(id)!!
        assertEquals(1, task.streak)
        assertEquals(today.toString(), task.lastCompletedDay)
    }

    @Test
    fun tapPlus_daily_isRejectedWhenNotDueToday() = runBlocking {
        val todayIndex = today.dayOfWeek.value - 1 // пн=0 … вс=6
        val otherDay = (todayIndex + 1) % 7
        val id = seedDaily(
            id = "weekly",
            startDate = LocalDate.of(2026, 11, 1),
            frequency = "WEEKLY",
            repeatDays = otherDay.toString(),
        )

        assertFalse("Задача не должна считаться нужной сегодня", repository.observeTasks(TaskType.DAILY).first().single().dueToday)
        assertNull(repository.tapPlus(id))
    }

    // --- разовые (F02) ---

    @Test
    fun completeTodo_rewardsOnlyOnce() = runBlocking {
        val id = repository.createTask(TaskType.TODO, "Позвонить", "", Difficulty.MEDIUM, up = true, down = false)

        assertNotNull(repository.completeTodo(id))
        assertNull(repository.completeTodo(id))

        val task = db.taskDao().getById(id)!!
        assertTrue(task.completed)
        assertEquals(Difficulty.MEDIUM.gold, db.userDao().getUser()!!.gold)
    }

    // --- ежедневный сброс (F06) ---

    @Test
    fun cron_missedDaily_resetsStreakAndHp() = runBlocking {
        val user = db.userDao().getUser()!!
        db.userDao().upsert(user.copy(lastCronDay = "2026-11-09"))
        seedDaily(
            id = "d1",
            startDate = LocalDate.of(2026, 11, 1),
            streak = 5,
            lastCompletedDay = "2026-11-08",
        )

        val report = repository.runDailyCron()

        assertNotNull(report)
        assertEquals(1, report!!.missedDailies)
        assertEquals(Difficulty.EASY.hpPenalty, report.hpLost)
        assertEquals(0, db.taskDao().getById("d1")!!.streak)
        val after = db.userDao().getUser()!!
        assertEquals(50 - Difficulty.EASY.hpPenalty, after.hp)
        assertEquals(today.toString(), after.lastCronDay)
    }

    @Test
    fun cron_isIdempotent_onSecondRunSameDay() = runBlocking {
        val user = db.userDao().getUser()!!
        db.userDao().upsert(user.copy(lastCronDay = "2026-11-09"))
        seedDaily("d1", LocalDate.of(2026, 11, 1), streak = 3, lastCompletedDay = "2026-11-08")

        val first = repository.runDailyCron()
        val hpAfterFirst = db.userDao().getUser()!!.hp
        val second = repository.runDailyCron()

        assertNotNull(first)
        assertNull("Повторный запуск в тот же день ничего не делает", second)
        assertEquals(hpAfterFirst, db.userDao().getUser()!!.hp)
    }

    @Test
    fun cron_resetsHabitCounters() = runBlocking {
        val user = db.userDao().getUser()!!
        db.userDao().upsert(user.copy(lastCronDay = "2026-11-09"))
        seedHabit("h1", counterUp = 4, counterDown = 2)

        repository.runDailyCron()

        val habit = db.taskDao().getById("h1")!!
        assertEquals(0, habit.counterUp)
        assertEquals(0, habit.counterDown)
    }

    /**
     * Регрессия BUG-001: если задача уже закрыта ПОЗЖЕ закрываемого дня, серия
     * пересчитана отметкой и сброс не должен её обнулять.
     */
    @Test
    fun cron_keepsStreak_whenTaskWasCompletedLater() = runBlocking {
        val user = db.userDao().getUser()!!
        db.userDao().upsert(user.copy(lastCronDay = "2026-11-08"))
        seedDaily(
            id = "d1",
            startDate = LocalDate.of(2026, 11, 1),
            streak = 1,
            lastCompletedDay = today.toString(),
        )

        val report = repository.runDailyCron()

        assertNotNull(report)
        assertEquals(2, report!!.missedDailies) // 11-08 и 11-09
        assertEquals(2 * Difficulty.EASY.hpPenalty, report.hpLost)
        assertEquals("Серия, заработанная сегодня, не должна обнуляться", 1, db.taskDao().getById("d1")!!.streak)
    }

    @Test
    fun cron_doesNothing_onFirstEverRun() = runBlocking {
        assertNull(repository.runDailyCron())
        assertEquals(today.toString(), db.userDao().getUser()!!.lastCronDay)
    }

    @Test
    fun cron_doesNotRollBack_whenClockMovesBackwards() = runBlocking {
        val user = db.userDao().getUser()!!
        db.userDao().upsert(user.copy(lastCronDay = "2026-11-15"))

        assertNull("Будущий last_cron_day не должен откатываться", repository.runDailyCron())
        assertEquals("2026-11-15", db.userDao().getUser()!!.lastCronDay)
    }

    @Test
    fun cron_reportsActualHpLoss_whenHpIsAlreadyZero() = runBlocking {
        val user = db.userDao().getUser()!!
        db.userDao().upsert(user.copy(hp = 0, lastCronDay = "2026-11-09"))
        seedDaily("d1", LocalDate.of(2026, 11, 1))

        val report = repository.runDailyCron()!!

        assertEquals(1, report.missedDailies)
        assertEquals("На нуле HP отчёт не должен обещать минус", 0, report.hpLost)
        assertEquals(0, db.userDao().getUser()!!.hp)
    }

    // --- целостность истории (BUG: REPLACE + CASCADE) ---

    @Test
    fun reinsertingSameTaskId_keepsTaskLogs() = runBlocking {
        val id = repository.createTask(TaskType.HABIT, "Привычка", "", Difficulty.EASY, up = true, down = true)
        repository.tapPlus(id)
        val before = db.taskLogDao().countAll()

        val task = db.taskDao().getById(id)!!
        db.taskDao().insert(task.copy(text = "Привычка 2"))

        assertEquals("Upsert не должен каскадно удалять историю", before, db.taskLogDao().countAll())
        assertEquals("Привычка 2", db.taskDao().getById(id)!!.text)
    }

    // --- helpers ---

    private suspend fun seedDaily(
        id: String,
        startDate: LocalDate,
        frequency: String = "DAILY",
        repeatDays: String = "",
        streak: Int = 0,
        lastCompletedDay: String? = null,
    ) {
        val user = db.userDao().getUser()!!
        db.taskDao().insert(
            TaskEntity(
                id = id,
                userId = user.id,
                type = TaskType.DAILY.name,
                text = "daily-$id",
                difficulty = Difficulty.EASY.name,
                frequency = frequency,
                repeatDays = repeatDays,
                streak = streak,
                lastCompletedDay = lastCompletedDay,
                startDate = startDate.toString(),
                createdAt = 0,
                updatedAt = 0,
            ),
        )
    }

    private suspend fun seedHabit(id: String, counterUp: Int, counterDown: Int) {
        val user = db.userDao().getUser()!!
        db.taskDao().insert(
            TaskEntity(
                id = id,
                userId = user.id,
                type = TaskType.HABIT.name,
                text = "habit-$id",
                difficulty = Difficulty.EASY.name,
                counterUp = counterUp,
                counterDown = counterDown,
                startDate = "2026-11-01",
                createdAt = 0,
                updatedAt = 0,
            ),
        )
    }
}
