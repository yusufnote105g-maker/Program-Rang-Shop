package com.example.habittracker.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.habittracker.data.local.entity.TagEntity
import com.example.habittracker.data.local.entity.TaskTagEntity
import kotlinx.coroutines.flow.Flow

/** Тег в разрезе задачи — плоская строка join-а task_tags + tags. */
data class TaskTagRow(
    @ColumnInfo(name = "task_id") val taskId: String,
    @ColumnInfo(name = "tag_id") val tagId: String,
    val name: String,
    val color: String,
)

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeTags(): Flow<List<TagEntity>>

    /**
     * Ищем без учёта регистра: если «Работа» уже есть, «работа» переиспользует её.
     * Уникальный индекс в SQLite регистрозависим, поэтому проверка живёт здесь.
     */
    @Query("SELECT * FROM tags WHERE user_id = :userId AND name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(userId: String, name: String): TagEntity?

    @Upsert
    suspend fun upsert(tag: TagEntity)

    @Query(
        """
        SELECT tt.task_id AS task_id, t.id AS tag_id, t.name AS name, t.color AS color
        FROM task_tags tt
        JOIN tags t ON t.id = tt.tag_id
        ORDER BY t.name COLLATE NOCASE
        """,
    )
    fun observeTaskTags(): Flow<List<TaskTagRow>>

    @Query(
        """
        SELECT t.* FROM tags t
        JOIN task_tags tt ON tt.tag_id = t.id
        WHERE tt.task_id = :taskId
        ORDER BY t.name COLLATE NOCASE
        """,
    )
    suspend fun getTagsForTask(taskId: String): List<TagEntity>

    @Query("DELETE FROM task_tags WHERE task_id = :taskId")
    suspend fun clearTaskTags(taskId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskTag(link: TaskTagEntity)

    @Query("SELECT COUNT(*) FROM task_tags WHERE task_id = :taskId")
    suspend fun countTaskTags(taskId: String): Int
}
