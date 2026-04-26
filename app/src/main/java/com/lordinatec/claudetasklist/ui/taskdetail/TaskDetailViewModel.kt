package com.lordinatec.claudetasklist.ui.taskdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lordinatec.claudetasklist.data.repository.TaskRepository
import com.lordinatec.claudetasklist.domain.model.Priority
import com.lordinatec.claudetasklist.domain.model.Tag
import com.lordinatec.claudetasklist.domain.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

const val NAV_ARG_TASK_ID = "taskId"

data class TaskDetailUiState(
    val title: String = "",
    val description: String = "",
    val dueDate: LocalDate? = null,
    val priority: Priority = Priority.MEDIUM,
    val selectedTags: List<Tag> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val isEditMode: Boolean = false,
    val isDirty: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val titleError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class TaskDetailUiEvent {
    data object NavigateBack : TaskDetailUiEvent()
}

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val repository: TaskRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: Long = savedStateHandle[NAV_ARG_TASK_ID] ?: -1L
    private var originalCreationDate: LocalDate = LocalDate.now()
    private var originalSnapshot: TaskDetailUiState? = null

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TaskDetailUiEvent>()
    val events: SharedFlow<TaskDetailUiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.getAllTags().collect { tags ->
                _uiState.update { it.copy(availableTags = tags) }
            }
        }
        if (taskId != -1L) {
            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                val task = repository.getTaskById(taskId).first()
                if (task != null) {
                    originalCreationDate = task.creationDate
                    val loaded = TaskDetailUiState(
                        title = task.title,
                        description = task.description ?: "",
                        dueDate = task.dueDate,
                        priority = task.priority,
                        selectedTags = task.tags,
                        availableTags = _uiState.value.availableTags,
                        isEditMode = true,
                        isDirty = false,
                        isLoading = false
                    )
                    _uiState.value = loaded
                    originalSnapshot = loaded
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        } else {
            originalSnapshot = _uiState.value
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, titleError = null, isDirty = computeIsDirty(it.copy(title = value))) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value, isDirty = computeIsDirty(it.copy(description = value))) }
    }

    fun onDueDateChange(date: LocalDate?) {
        _uiState.update { it.copy(dueDate = date, isDirty = computeIsDirty(it.copy(dueDate = date))) }
    }

    fun onPriorityChange(priority: Priority) {
        _uiState.update { it.copy(priority = priority, isDirty = computeIsDirty(it.copy(priority = priority))) }
    }

    fun onTagToggle(tag: Tag) {
        _uiState.update { current ->
            val updated = if (tag in current.selectedTags) current.selectedTags - tag else current.selectedTags + tag
            current.copy(selectedTags = updated, isDirty = computeIsDirty(current.copy(selectedTags = updated)))
        }
    }

    fun onSaveClick() = viewModelScope.launch {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "Title is required") }
            return@launch
        }
        _uiState.update { it.copy(isLoading = true) }
        val task = buildTask(state)
        val result = if (state.isEditMode) repository.updateTask(task)
                     else repository.insertTask(task).map { }
        result
            .onSuccess { _events.emit(TaskDetailUiEvent.NavigateBack) }
            .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message, isLoading = false) } }
    }

    fun onBackClick() = viewModelScope.launch {
        if (_uiState.value.isDirty) _uiState.update { it.copy(showDiscardDialog = true) }
        else _events.emit(TaskDetailUiEvent.NavigateBack)
    }

    fun onDiscardConfirmed() = viewModelScope.launch { _events.emit(TaskDetailUiEvent.NavigateBack) }
    fun onDiscardCancelled() { _uiState.update { it.copy(showDiscardDialog = false) } }
    fun onErrorDismissed() { _uiState.update { it.copy(errorMessage = null) } }

    private fun buildTask(state: TaskDetailUiState) = Task(
        id = if (state.isEditMode) taskId else 0L,
        title = state.title.trim(),
        description = state.description.takeIf { it.isNotBlank() },
        dueDate = state.dueDate,
        priority = state.priority,
        tags = state.selectedTags,
        creationDate = originalCreationDate
    )

    private fun computeIsDirty(current: TaskDetailUiState): Boolean {
        val snapshot = originalSnapshot ?: return current.title.isNotBlank() ||
                current.description.isNotBlank() || current.dueDate != null ||
                current.priority != Priority.MEDIUM || current.selectedTags.isNotEmpty()
        return current.title != snapshot.title ||
                current.description != snapshot.description ||
                current.dueDate != snapshot.dueDate ||
                current.priority != snapshot.priority ||
                current.selectedTags.map { it.id }.toSet() != snapshot.selectedTags.map { it.id }.toSet()
    }
}
