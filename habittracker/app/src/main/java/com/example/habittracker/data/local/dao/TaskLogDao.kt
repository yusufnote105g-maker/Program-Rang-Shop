package com.example.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.habittracker.data.local.entity.TaskLogEntity

@Dao
interface TaskLogDao {

    @Insert
    suspend fun insert(log: TaskLogEntity)

    /** Для тестов и будущей аналитики: сколько всего отметок сохранено. */
    @Query("SELECT COUNT(*) FROM task_logs")
    suspend fun countAll(): Int
}
