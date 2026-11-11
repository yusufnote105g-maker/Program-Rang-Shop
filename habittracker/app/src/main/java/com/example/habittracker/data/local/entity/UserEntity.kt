package com.example.habittracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Таблица users. В локальном приложении одна строка (один игрок). */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val level: Int = 1,
    val xp: Int = 0,
    val hp: Int = 50,
    @ColumnInfo(name = "max_hp") val maxHp: Int = 50,
    val gold: Int = 0,
    val gems: Int = 0,
    // Свойство нельзя назвать `class` (ключевое слово Kotlin), поэтому column явно "class".
    @ColumnInfo(name = "class") val characterClass: String = "none",
    val timezone: String = "UTC",
    @ColumnInfo(name = "day_start_minutes") val dayStartMinutes: Int = 0,
    @ColumnInfo(name = "last_cron_day") val lastCronDay: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
