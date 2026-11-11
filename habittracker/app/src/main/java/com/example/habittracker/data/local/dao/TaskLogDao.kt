package com.example.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.habittracker.data.local.entity.TaskLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskLogDao {

    @Insert
    suspend fun insert(log: TaskLogEntity)

    /** Для тестов и будущей аналитики: сколько всего отметок сохранено. */
    @Query("SELECT COUNT(*) FROM task_logs")
    suspend fun countAll(): Int

    @Query(
        """
        SELECT logged_day AS day,
               SUM(CASE WHEN direction IN ('up', 'complete') THEN 1 ELSE 0 END) AS completions,
               SUM(CASE WHEN direction = 'miss' THEN 1 ELSE 0 END) AS misses,
               SUM(CASE WHEN delta_xp > 0 THEN delta_xp ELSE 0 END) AS xp_earned,
               SUM(CASE WHEN delta_gold > 0 THEN delta_gold ELSE 0 END) AS gold_earned
        FROM task_logs
        WHERE logged_day >= :from AND logged_day <= :to
        GROUP BY logged_day
        ORDER BY logged_day
        """,
    )
    fun observeDaily(from: String, to: String): Flow<List<DailyLogRow>>
}
