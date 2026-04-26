package com.lordinatec.claudetasklist.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lordinatec.claudetasklist.data.repository.TaskRepository
import com.lordinatec.claudetasklist.domain.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class SearchUiEvent {
    data class NavigateToDetail(val taskId: Long) : SearchUiEvent()
    data object CopySuccess : SearchUiEvent()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SearchUiEvent>()
    val events: SharedFlow<SearchUiEvent> = _events.asSharedFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(300L)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isBlank()) flowOf(emptyList()) else repository.searchTasks(query)
                }
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { results -> _uiState.update { it.copy(results = results) } }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        queryFlow.value = query
    }

    fun onTaskClick(task: Task) = viewModelScope.launch {
        _events.emit(SearchUiEvent.NavigateToDetail(task.id))
    }

    fun onCopyTask(task: Task) = viewModelScope.launch {
        repository.copyAsNewTask(task)
            .onSuccess { _events.emit(SearchUiEvent.CopySuccess) }
            .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
    }

    fun onErrorDismissed() { _uiState.update { it.copy(errorMessage = null) } }
}
