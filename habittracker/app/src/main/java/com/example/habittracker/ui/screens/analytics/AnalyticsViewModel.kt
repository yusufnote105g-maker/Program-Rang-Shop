package com.example.habittracker.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.repository.TaskRepository
import com.example.habittracker.domain.model.AnalyticsData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AnalyticsUiState(
    val data: AnalyticsData? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    repository: TaskRepository,
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = repository.observeAnalytics(RANGE_DAYS)
        .map { data -> AnalyticsUiState(data = data, loading = false) }
        .catch { emit(AnalyticsUiState(loading = false, error = true)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AnalyticsUiState(),
        )

    companion object {
        const val RANGE_DAYS = 30
    }
}
