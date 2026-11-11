package com.example.habittracker.ui.screens.taskform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.repository.TaskRepository
import com.example.habittracker.domain.model.Difficulty
import com.example.habittracker.domain.model.Tag
import com.example.habittracker.domain.model.TaskType
import com.example.habittracker.domain.tags.TagRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskFormUiState(
    val isEdit: Boolean = false,
    val type: TaskType = TaskType.HABIT,
    val name: String = "",
    val notes: String = "",
    val difficulty: Difficulty = Difficulty.EASY,
    val availableTags: List<Tag> = emptyList(),
    val selectedTags: List<String> = emptyList(),
    val tagDraft: String = "",
    val tagError: TagRules.Error? = null,
    val nameError: Boolean = false,
    val loading: Boolean = false,
    val loadFailed: Boolean = false,
    val saving: Boolean = false,
    val deleting: Boolean = false,
    val saveError: Boolean = false,
    val deleteError: Boolean = false,
)

sealed interface TaskFormEvent {
    data object Saved : TaskFormEvent
    data object Deleted : TaskFormEvent
}

/**
 * S02 — создание, S06 — редактирование. Одна форма на оба режима:
 * поля совпадают, отличается только загрузка/сохранение.
 *
 * В режиме создания черновик живёт в SavedStateHandle (переживает убийство
 * процесса). В режиме редактирования источник правды — БД: при пересоздании
 * процесса форма заново загрузит задачу, а несохранённые правки потеряются.
 */
@HiltViewModel
class TaskFormViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** null — создание; иначе id редактируемой задачи (S06). */
    private val taskId: String? = savedStateHandle[ARG_TASK_ID]
    private val isEdit: Boolean = taskId != null

    private val initialType: TaskType = savedStateHandle.get<String>(KEY_TYPE)
        ?.let { raw -> TaskType.entries.firstOrNull { it.name == raw } }
        ?: savedStateHandle.get<String>(ARG_TYPE)
            ?.let { raw -> TaskType.entries.firstOrNull { it.name == raw } }
        ?: TaskType.HABIT

    private val _state = MutableStateFlow(
        if (isEdit) {
            TaskFormUiState(isEdit = true, loading = true)
        } else {
            TaskFormUiState(
                isEdit = false,
                type = initialType,
                name = savedStateHandle[KEY_NAME] ?: "",
                notes = savedStateHandle[KEY_NOTES] ?: "",
                difficulty = savedStateHandle.get<String>(KEY_DIFFICULTY)
                    ?.let { raw -> Difficulty.entries.firstOrNull { it.name == raw } }
                    ?: Difficulty.EASY,
                selectedTags = savedStateHandle.get<ArrayList<String>>(KEY_TAGS)?.toList().orEmpty(),
            )
        },
    )
    val state: StateFlow<TaskFormUiState> = _state.asStateFlow()

    private val _events = Channel<TaskFormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            try {
                repository.observeTags().collect { tags ->
                    _state.update { it.copy(availableTags = tags) }
                }
            } catch (c: CancellationException) {
                throw c
            } catch (_: Throwable) {
                // Теги — необязательная часть: форма работает и без подсказок из БД.
            }
        }
        if (isEdit) loadTask()
    }

    fun setType(type: TaskType) {
        if (isEdit) return // тип после создания не меняется
        savedStateHandle[KEY_TYPE] = type.name
        _state.update { it.copy(type = type) }
    }

    fun setName(value: String) {
        val limited = value.limitCodePoints(MAX_NAME_LENGTH)
        if (!isEdit) savedStateHandle[KEY_NAME] = limited
        _state.update { it.copy(name = limited, nameError = false) }
    }

    fun setNotes(value: String) {
        val limited = value.limitCodePoints(MAX_NOTES_LENGTH)
        if (!isEdit) savedStateHandle[KEY_NOTES] = limited
        _state.update { it.copy(notes = limited) }
    }

    fun setDifficulty(difficulty: Difficulty) {
        if (!isEdit) savedStateHandle[KEY_DIFFICULTY] = difficulty.name
        _state.update { it.copy(difficulty = difficulty) }
    }

    fun setTagDraft(value: String) {
        val limited = value.limitCodePoints(MAX_TAG_INPUT)
        _state.update { it.copy(tagDraft = limited, tagError = null) }
    }

    /** Добавляет введённый тег к выбранным (кнопка «+» или IME Done). */
    fun addTag() {
        val current = _state.value
        val error = TagRules.validateNew(current.tagDraft, current.selectedTags)
        if (error != null) {
            _state.update { it.copy(tagError = error) }
            return
        }
        updateSelectedTags(current.selectedTags + current.tagDraft.trim(), clearDraft = true)
    }

    /** Выбирает/снимает тег из подсказок. */
    fun toggleTag(name: String) {
        val current = _state.value
        val existing = current.selectedTags.firstOrNull { it.equals(name, ignoreCase = true) }
        if (existing != null) {
            updateSelectedTags(current.selectedTags - existing)
            return
        }
        if (current.selectedTags.size >= TagRules.MAX_TAGS) {
            _state.update { it.copy(tagError = TagRules.Error.LIMIT) }
            return
        }
        updateSelectedTags(current.selectedTags + name)
    }

    fun clearSaveError() = _state.update { it.copy(saveError = false) }

    fun clearDeleteError() = _state.update { it.copy(deleteError = false) }

    fun save() {
        val current = _state.value
        // Защита от двойного тапа: второй вызов приходит до перерисовки кнопки.
        if (current.saving || current.deleting || current.loading || current.loadFailed) return
        if (current.name.isBlank()) {
            _state.update { it.copy(nameError = true) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                repository.ensureUser()
                val id = taskId
                if (id == null) {
                    repository.createTask(
                        type = current.type,
                        text = current.name,
                        notes = current.notes,
                        difficulty = current.difficulty,
                        up = true,
                        down = current.type == TaskType.HABIT,
                        tags = current.selectedTags,
                    )
                    // Задача уже в БД: чистим черновик, чтобы после убийства процесса
                    // до возврата на список форма не сохранила дубль повторным Save.
                    savedStateHandle[KEY_NAME] = ""
                    savedStateHandle[KEY_NOTES] = ""
                    savedStateHandle[KEY_TAGS] = ArrayList<String>()
                    _state.update {
                        it.copy(
                            name = "",
                            notes = "",
                            selectedTags = emptyList(),
                            tagDraft = "",
                            saving = false,
                        )
                    }
                } else {
                    val updated = repository.updateTask(
                        taskId = id,
                        text = current.name,
                        notes = current.notes,
                        difficulty = current.difficulty,
                        tags = current.selectedTags,
                    )
                    if (!updated) {
                        // Задачу удалили, пока форма была открыта.
                        _state.update { it.copy(saving = false, loadFailed = true) }
                        return@launch
                    }
                    _state.update { it.copy(saving = false) }
                }
                _events.send(TaskFormEvent.Saved)
            } catch (c: CancellationException) {
                // Отмена ViewModel — не ошибка сохранения, пробрасываем дальше.
                throw c
            } catch (_: Throwable) {
                _state.update { it.copy(saving = false, saveError = true) }
            }
        }
    }

    /** S06: удаление прямо из формы, после подтверждения в UI. */
    fun delete() {
        val id = taskId ?: return
        val current = _state.value
        if (current.deleting || current.saving) return
        _state.update { it.copy(deleting = true) }
        viewModelScope.launch {
            try {
                repository.deleteTask(id)
                _state.update { it.copy(deleting = false) }
                _events.send(TaskFormEvent.Deleted)
            } catch (c: CancellationException) {
                throw c
            } catch (_: Throwable) {
                _state.update { it.copy(deleting = false, deleteError = true) }
            }
        }
    }

    private fun loadTask() {
        val id = taskId ?: return
        viewModelScope.launch {
            try {
                val task = repository.loadTask(id)
                if (task == null) {
                    _state.update { it.copy(loading = false, loadFailed = true) }
                    return@launch
                }
                _state.update {
                    it.copy(
                        loading = false,
                        type = task.type,
                        name = task.text,
                        notes = task.notes,
                        difficulty = task.difficulty,
                        selectedTags = task.tags.map { tag -> tag.name },
                    )
                }
            } catch (c: CancellationException) {
                throw c
            } catch (_: Throwable) {
                _state.update { it.copy(loading = false, loadFailed = true) }
            }
        }
    }

    private fun updateSelectedTags(tags: List<String>, clearDraft: Boolean = false) {
        val normalized = TagRules.normalize(tags)
        if (!isEdit) savedStateHandle[KEY_TAGS] = ArrayList(normalized)
        _state.update {
            it.copy(
                selectedTags = normalized,
                tagDraft = if (clearDraft) "" else it.tagDraft,
                tagError = null,
            )
        }
    }

    companion object {
        const val ARG_TYPE = "type"
        const val ARG_TASK_ID = "taskId"

        private const val KEY_TYPE = "form_type"
        private const val KEY_NAME = "form_name"
        private const val KEY_NOTES = "form_notes"
        private const val KEY_DIFFICULTY = "form_difficulty"
        private const val KEY_TAGS = "form_tags"

        /** Лимит названия: длиннее всё равно не прочитать в строке списка. */
        private const val MAX_NAME_LENGTH = 120

        /** Заметки тоже ограничиваем, чтобы Bundle не раздувался. */
        private const val MAX_NOTES_LENGTH = 2_000

        /** Чуть больше лимита тега, чтобы успеть показать ошибку «слишком длинный». */
        private const val MAX_TAG_INPUT = 40
    }
}

/** Обрезает строку по code points, чтобы не разорвать эмодзи пополам. */
private fun String.limitCodePoints(max: Int): String =
    if (codePointCount(0, length) <= max) this
    else substring(0, offsetByCodePoints(0, max))
