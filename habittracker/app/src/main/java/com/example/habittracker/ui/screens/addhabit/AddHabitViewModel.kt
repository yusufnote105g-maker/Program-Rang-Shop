package com.example.habittracker.ui.screens.addhabit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.repository.TaskRepository
import com.example.habittracker.domain.model.Difficulty
import com.example.habittracker.domain.model.TaskType
import dagger.hilt.android.lifecycle.HiltViewModel
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
)

@HiltViewModel
class AddHabitViewModel @Inject constructor(
    private val repository: TaskRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddHabitUiState())
    val state = _state.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    fun setType(type: TaskType) = _state.update { it.copy(type = type) }

    fun setName(value: String) = _state.update { it.copy(name = value, nameError = false) }

    fun setNotes(value: String) = _state.update { it.copy(notes = value) }

    fun setDifficulty(difficulty: Difficulty) = _state.update { it.copy(difficulty = difficulty) }

    fun save() {
        val current = _state.value
        if (current.name.isBlank()) {
            _state.update { it.copy(nameError = true) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.ensureUser()
            repository.createTask(
                type = current.type,
                text = current.name,
                notes = current.notes,
                difficulty = current.difficulty,
                up = true,
                down = current.type == TaskType.HABIT,
            )
            _state.update { it.copy(saving = false) }
            _saved.send(Unit)
        }
    }
}
