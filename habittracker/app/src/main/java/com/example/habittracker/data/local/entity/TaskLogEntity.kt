package com.example.habittracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Журнал отметок: история наград, нужна для аналитики и отката. */
@Entity(
    tableName = "task_logs",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["task_id", "logged_at"]),
        Index(value = ["user_id", "logged_day"]),
    ],
)
data class TaskLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "task_id") val taskId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "logged_at") val loggedAt: Long,
    @ColumnInfo(name = "logged_day") val loggedDay: String,
    val direction: String, // up | down | complete | miss
    @ColumnInfo(name = "delta_xp") val deltaXp: Int,
    @ColumnInfo(name = "delta_gold") val deltaGold: Int,
    val value: Double,
)
