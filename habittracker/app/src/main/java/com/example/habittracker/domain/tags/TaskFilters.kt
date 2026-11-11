package com.example.habittracker.domain.tags

import com.example.habittracker.domain.model.Tag
import com.example.habittracker.domain.model.Task
import java.util.Locale

/**
 * Фильтр и группировка S01. Чистые функции, чтобы поведение можно было
 * проверить JVM-тестами без Compose и Room.
 */
data class TaskGroup(
    /** null — группа «Без тега». */
    val tag: Tag?,
    val tasks: List<Task>,
)

/** OR-семантика: задача видна, если у неё есть хотя бы один выбранный тег. */
fun filterTasks(tasks: List<Task>, selectedTags: Set<String>): List<Task> {
    if (selectedTags.isEmpty()) return tasks
    val keys = selectedTags.map { it.lowercase(Locale.ROOT) }.toSet()
    return tasks.filter { task ->
        task.tags.any { it.name.lowercase(Locale.ROOT) in keys }
    }
}

/**
 * Задача с несколькими тегами показывается в каждой группе — так пользователь
 * находит её под любым тегом. Группа «Без тега» всегда последняя, остальные
 * отсортированы по имени без учёта регистра.
 */
fun groupTasks(tasks: List<Task>): List<TaskGroup> {
    val groups = LinkedHashMap<String, Pair<Tag?, MutableList<Task>>>()
    fun add(tag: Tag?, key: String, task: Task) {
        val entry = groups.getOrPut(key) { tag to mutableListOf() }
        entry.second += task
    }

    tasks.forEach { task ->
        if (task.tags.isEmpty()) {
            add(null, UNTAGGED_KEY, task)
        } else {
            task.tags.forEach { tag ->
                add(tag, tag.name.lowercase(Locale.ROOT), task)
            }
        }
    }

    return groups.values
        .map { (tag, list) -> TaskGroup(tag = tag, tasks = list) }
        .sortedWith(
            compareBy<TaskGroup> { it.tag == null }
                .thenBy { it.tag?.name?.lowercase(Locale.ROOT) ?: "" },
        )
}

private const val UNTAGGED_KEY = "\u0000untagged"
