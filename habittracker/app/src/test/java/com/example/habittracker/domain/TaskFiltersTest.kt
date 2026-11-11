package com.example.habittracker.domain

import com.example.habittracker.domain.model.Tag
import com.example.habittracker.domain.model.Task
import com.example.habittracker.domain.model.TaskType
import com.example.habittracker.domain.tags.filterTasks
import com.example.habittracker.domain.tags.groupTasks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskFiltersTest {

    private fun task(id: String, text: String, vararg tags: String): Task = Task(
        id = id,
        type = TaskType.HABIT,
        text = text,
        tags = tags.mapIndexed { index, name ->
            Tag(id = "$id-$index", name = name, color = "#3A4ED8")
        },
    )

    @Test
    fun filter_emptySelectionReturnsEverything() {
        val tasks = listOf(task("1", "Бег", "спорт"), task("2", "Чтение"))
        assertEquals(tasks, filterTasks(tasks, emptySet()))
    }

    @Test
    fun filter_isCaseInsensitiveAndUsesOrSemantics() {
        val tasks = listOf(
            task("1", "Бег", "Спорт"),
            task("2", "Чтение", "вечер"),
            task("3", "Код", "работа"),
        )
        val filtered = filterTasks(tasks, setOf("спорт", "РАБОТА"))
        assertEquals(setOf("1", "3"), filtered.map { it.id }.toSet())
    }

    @Test
    fun group_putsUntaggedLastAndSortsByName() {
        val tasks = listOf(
            task("1", "Без тега"),
            task("2", "Работа", "Работа"),
            task("3", "Спорт", "спорт"),
        )
        val groups = groupTasks(tasks)
        assertEquals(listOf("Работа", "спорт", null), groups.map { it.tag?.name })
        assertEquals("1", groups.last().tasks.single().id)
    }

    @Test
    fun group_showsMultiTagTaskInEveryGroup() {
        val tasks = listOf(task("1", "Утро", "здоровье", "дом"))
        val groups = groupTasks(tasks)
        assertEquals(2, groups.size)
        assertTrue(groups.all { it.tasks.single().id == "1" })
    }

    @Test
    fun group_emptyInputIsEmpty() {
        assertTrue(groupTasks(emptyList()).isEmpty())
    }
}
