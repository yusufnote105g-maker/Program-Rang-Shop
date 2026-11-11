package com.example.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.habittracker.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE type = :type AND archived = 0 ORDER BY position, created_at")
    fun observeByType(type: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE archived = 0 ORDER BY position, created_at")
    suspend fun getAllActive(): List<TaskEntity>

    /** Все задачи с включённым напоминанием — для восстановления после перезагрузки. */
    @Query("SELECT * FROM tasks WHERE archived = 0 AND reminder_enabled = 1")
    suspend fun getEnabledReminders(): List<TaskEntity>

    @Query(
        """
        SELECT type, COUNT(*) AS total,
               SUM(CASE WHEN completed = 1 THEN 1 ELSE 0 END) AS completed
        FROM tasks WHERE archived = 0
        GROUP BY type
        """,
    )
    fun observeTypeCounts(): Flow<List<TaskTypeCountRow>>

    @Query(
        """
        SELECT id, text, streak, best_streak AS best_streak
        FROM tasks
        WHERE archived = 0 AND type = 'DAILY'
        ORDER BY best_streak DESC, streak DESC
        LIMIT 5
        """,
    )
    fun observeTopStreaks(): Flow<List<TaskStreakRow>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    /** Все незаархивированные ежедневные — их пересчитывает cron. */
    @Query("SELECT * FROM tasks WHERE type = 'DAILY' AND archived = 0")
    suspend fun getDailies(): List<TaskEntity>

    /**
     * Upsert, а не REPLACE: REPLACE удаляет старую строку и запускает
     * ON DELETE CASCADE, из-за чего при повторной вставке того же id
     * молча пропадала бы вся история из task_logs.
     */
    @Upsert
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM tasks WHERE user_id = :userId")
    suspend fun nextPosition(userId: String): Int

    /** Дневные счётчики привычек не переносятся на новый день. */
    @Query("UPDATE tasks SET counter_up = 0, counter_down = 0, updated_at = :now WHERE type = 'HABIT'")
    suspend fun resetHabitCounters(now: Long)

    /** Обнуляет серию у ежедневных, которые были пропущены. */
    @Query("UPDATE tasks SET streak = 0, updated_at = :now WHERE id IN (:ids)")
    suspend fun resetStreaks(ids: List<String>, now: Long)
}
