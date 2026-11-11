package com.example.habittracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Таблица tasks: habit / daily / todo в одной таблице.
 * Enum-поля храним строками (значения enum .name), чтобы не тянуть конвертеры.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["user_id", "type", "archived", "position"]),
        Index(value = ["reminder_enabled", "reminder_at"]),
    ],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val type: String,
    val text: String,
    val notes: String = "",
    val difficulty: String = "EASY",
    // habit
    val up: Boolean = true,
    val down: Boolean = false,
    @ColumnInfo(name = "counter_up") val counterUp: Int = 0,
    @ColumnInfo(name = "counter_down") val counterDown: Int = 0,
    // daily
    val streak: Int = 0,
    @ColumnInfo(name = "best_streak") val bestStreak: Int = 0,
    val frequency: String = "DAILY",
    @ColumnInfo(name = "every_x") val everyX: Int = 1,
    @ColumnInfo(name = "repeat_days") val repeatDays: String = "",
    @ColumnInfo(name = "start_date") val startDate: String,
    @ColumnInfo(name = "last_completed_day") val lastCompletedDay: String? = null,
    // todo
    @ColumnInfo(name = "due_date") val dueDate: String? = null,
    val completed: Boolean = false,
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    // общее
    @ColumnInfo(name = "reminder_at") val reminderAt: Long? = null,
    @ColumnInfo(name = "reminder_enabled") val reminderEnabled: Boolean = false,
    val position: Int = 0,
    val archived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
