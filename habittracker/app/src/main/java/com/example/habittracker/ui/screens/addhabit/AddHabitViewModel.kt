package com.example.habittracker.ui.screens.addhabit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.repository.TaskRepository
import com.example.habittracker.domain.model.Difficulty
import com.example.habittracker.domain.model.TaskType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddHabitUiState(
    val type: TaskType = TaskType.HABIT,
    val name: String = "",
    val notes: String = "",
    val difficulty: Difficulty = Difficulty.EASY,
    val nameError: Boolean = false,
    val saving: Boolean = false,
    val saveError: Boolean = false,
)

@HiltViewModel
class AddHabitViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Тип приходит из вкладки, с которой нажали «+». */
    private val initialType: TaskType = savedStateHandle.get<String>(KEY_TYPE)
        ?.let { raw -> TaskType.entries.firstOrNull { it.name == raw } }
        ?: savedStateHandle.get<String>(ARG_TYPE)
            ?.let { raw -> TaskType.entries.firstOrNull { it.name == raw } }
        ?: TaskType.HABIT

    /**
     * Поля формы живут и в SavedStateHandle: после убийства процесса пользователь
     * не теряет уже введённое название и заметки.
     */
    private val _state = MutableStateFlow(
        AddHabitUiState(
            type = initialType,
            name = savedStateHandle[KEY_NAME] ?: "",
            notes = savedStateHandle[KEY_NOTES] ?: "",
            difficulty = savedStateHandle.get<String>(KEY_DIFFICULTY)
                ?.let { raw -> Difficulty.entries.firstOrNull { it.name == raw } }
                ?: Difficulty.EASY,
        ),
    )
    val state = _state.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    fun setType(type: TaskType) {
        savedStateHandle[KEY_TYPE] = type.name
        _state.update { it.copy(type = type) }
    }

    fun setName(value: String) {
        val limited = value.limitCodePoints(MAX_NAME_LENGTH)
        savedStateHandle[KEY_NAME] = limited
        _state.update { it.copy(name = limited, nameError = false) }
    }

    fun setNotes(value: String) {
        val limited = value.limitCodePoints(MAX_NOTES_LENGTH)
        savedStateHandle[KEY_NOTES] = limited
        _state.update { it.copy(notes = limited) }
    }

    fun setDifficulty(difficulty: Difficulty) {
        savedStateHandle[KEY_DIFFICULTY] = difficulty.name
        _state.update { it.copy(difficulty = difficulty) }
    }

    fun clearSaveError() = _state.update { it.copy(saveError = false) }

    fun save() {
        val current = _state.value
        // Защита от двойного тапа: второй вызов приходит до перерисовки кнопки.
        if (current.saving) return
        if (current.name.isBlank()) {
            _state.update { it.copy(nameError = true) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                repository.ensureUser()
                repository.createTask(
                    type = current.type,
                    text = current.name,
                    notes = current.notes,
                    difficulty = current.difficulty,
                    up = true,
                    down = current.type == TaskType.HABIT,
                )
                // Задача уже в БД: чистим черновик, чтобы после убийства процесса
                // до возврата на список форма не сохранила дубль повторным Save.
                savedStateHandle[KEY_NAME] = ""
                savedStateHandle[KEY_NOTES] = ""
                _state.update { it.copy(name = "", notes = "", saving = false) }
                _saved.send(Unit)
            } catch (c: CancellationException) {
                // Отмена ViewModel — не ошибка сохранения, пробрасываем дальше.
                throw c
            } catch (t: Throwable) {
                _state.update { it.copy(saving = false, saveError = true) }
            }
        }
    }

    companion object {
        const val ARG_TYPE = "type"

        private const val KEY_TYPE = "form_type"
        private const val KEY_NAME = "form_name"
        private const val KEY_NOTES = "form_notes"
        private const val KEY_DIFFICULTY = "form_difficulty"

        /** Лимит названия: длиннее всё равно не прочитать в строке списка. */
        private const val MAX_NAME_LENGTH = 120

        /** Заметки тоже ограничиваем, чтобы Bundle не раздувался. */
        private const val MAX_NOTES_LENGTH = 2_000
    }
}

/** Обрезает строку по code points, чтобы не разорвать эмодзи пополам. */
private fun String.limitCodePoints(max: Int): String =
    if (codePointCount(0, length) <= max) this
    else substring(0, offsetByCodePoints(0, max))
