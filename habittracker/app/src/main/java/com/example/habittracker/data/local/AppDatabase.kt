package com.example.habittracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.habittracker.data.local.dao.TagDao
import com.example.habittracker.data.local.dao.TaskDao
import com.example.habittracker.data.local.dao.TaskLogDao
import com.example.habittracker.data.local.dao.UserDao
import com.example.habittracker.data.local.entity.TagEntity
import com.example.habittracker.data.local.entity.TaskEntity
import com.example.habittracker.data.local.entity.TaskLogEntity
import com.example.habittracker.data.local.entity.TaskTagEntity
import com.example.habittracker.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        TaskEntity::class,
        TagEntity::class,
        TaskTagEntity::class,
        TaskLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun taskDao(): TaskDao
    abstract fun tagDao(): TagDao
    abstract fun taskLogDao(): TaskLogDao

    companion object {
        const val NAME = "habittracker.db"
    }
}
