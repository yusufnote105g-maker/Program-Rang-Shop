package com.example.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import com.example.habittracker.data.local.entity.TaskLogEntity

@Dao
interface TaskLogDao {

    @Insert
    suspend fun insert(log: TaskLogEntity)
}
