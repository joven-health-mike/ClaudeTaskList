package com.lordinatec.claudetasklist.ui.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lordinatec.claudetasklist.data.repository.TaskRepository
import com.lordinatec.claudetasklist.domain.model.Tag
import com.lordinatec.claudetasklist.domain.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOrder { CREATION_DATE, DUE_DATE, PRIORITY }

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val activeFilter: Tag? = null,
    val activeSortOrder: SortOrder = SortOrder.CREATION_DATE,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class TaskListUiEvent {
    data object NavigateToNewTask : TaskListUiEvent()
    data class NavigateToDetail(val taskId: Long) : TaskListUiEvent()
    data object NavigateToSearch : TaskListUiEvent()
}

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _activeFilter = MutableStateFlow<Tag?>(null)
    private val _activeSortOrder = MutableStateFlow(SortOrder.CREATION_DATE)
    private val _uiState = MutableStateFlow(TaskListUiState(isLoading = true))
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TaskListUiEvent>()
    val events: SharedFlow<TaskListUiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAllTasks(),
                repository.getAllTags(),
                _activeFilter,
                _activeSortOrder
            ) { tasks, tags, filter, sortOrder ->
                val filtered = if (filter == null) tasks else tasks.filter { filter in it.tags }
                val sorted = sortTasks(filtered, sortOrder)
                _uiState.value.copy(
                    tasks = sorted,
                    allTags = tags,
                    activeFilter = filter,
                    activeSortOrder = sortOrder,
                    isLoading = false
                )
            }.catch { e ->
                emit(_uiState.value.copy(errorMessage = e.message, isLoading = false))
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onCreateTaskClick() = viewModelScope.launch { _events.emit(TaskListUiEvent.NavigateToNewTask) }
    fun onTaskClick(task: Task) = viewModelScope.launch { _events.emit(TaskListUiEvent.NavigateToDetail(task.id)) }
    fun onSearchClick() = viewModelScope.launch { _events.emit(TaskListUiEvent.NavigateToSearch) }

    fun onToggleCompletion(task: Task) = viewModelScope.launch {
        repository.toggleTaskCompletion(task).onFailure { e ->
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun onDeleteTask(task: Task) = viewModelScope.launch {
        repository.deleteTask(task).onFailure { e ->
            _uiState.update { it.copy(errorMessage = e.message) }
        }
    }

    fun onFilterByTag(tag: Tag?) { _activeFilter.value = tag }
    fun onSortOrderChange(sortOrder: SortOrder) { _activeSortOrder.value = sortOrder }
    fun onErrorDismissed() { _uiState.update { it.copy(errorMessage = null) } }

    private fun sortTasks(tasks: List<Task>, sortOrder: SortOrder): List<Task> = when (sortOrder) {
        SortOrder.CREATION_DATE -> tasks.sortedByDescending { it.creationDate }
        SortOrder.DUE_DATE -> tasks.sortedWith(compareBy(nullsLast()) { it.dueDate })
        SortOrder.PRIORITY -> tasks.sortedByDescending { it.priority.ordinal }
    }
}
