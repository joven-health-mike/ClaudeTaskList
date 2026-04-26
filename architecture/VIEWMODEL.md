# ClaudeTaskList — ViewModel Layer Architecture

## Overview

There are three ViewModels, one per screen. Each follows the same structural contract:

- A single `uiState: StateFlow<UiState>` that Composables observe
- A `events: SharedFlow<UiEvent>` for one-shot navigation and imperative signals
- Plain functions for user actions — no sealed `Intent` classes

```
Composable  ──► fun onXxx()  ──►  ViewModel  ──►  TaskRepository
               (user action)       │
                                   ├── uiState: StateFlow   ◄── Composable observes
                                   └── events: SharedFlow   ◄── Composable collects (once)
```

All ViewModels are annotated with `@HiltViewModel` and injected by Hilt.

---

## Shared Conventions

| Convention | Detail |
|---|---|
| One-shot events | `SharedFlow<UiEvent>` with `replay = 0`; collected in `LaunchedEffect` in the Composable |
| Repository errors | `Result.onFailure { }` sets `errorMessage: String?` in `UiState`; cleared after display |
| Loading state | `isLoading: Boolean` in `UiState`; set to `true` before async calls, `false` in `finally` |
| IO work | Launched in `viewModelScope`; Repository enforces `Dispatchers.IO` internally |
| UiState colocation | Each `UiState` and `UiEvent` sealed class is defined in the same file as its ViewModel |

---

## TaskListViewModel

**File:** `ui/tasklist/TaskListViewModel.kt`

### UiState

```kotlin
data class TaskListUiState(
    val tasks: List<Task> = emptyList(),       // filtered + sorted result
    val allTags: List<Tag> = emptyList(),
    val activeFilter: Tag? = null,             // null = all tags shown
    val activeSortOrder: SortOrder = SortOrder.CREATION_DATE,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

enum class SortOrder { CREATION_DATE, DUE_DATE, PRIORITY }
```

`tasks` is always the **derived** list — it is never set directly. The ViewModel combines the raw `Flow<List<Task>>` from the repository with `activeFilter` and `activeSortOrder` using `combine`, then applies in-memory filter and sort.

```
repository.getAllTasks()  ─┐
activeFilter (state)      ─┼─►  combine  ──►  filtered + sorted  ──►  uiState.tasks
activeSortOrder (state)   ─┘
```

### UiEvent

```kotlin
sealed class TaskListUiEvent {
    object NavigateToNewTask : TaskListUiEvent()
    data class NavigateToDetail(val taskId: Long) : TaskListUiEvent()
    object NavigateToSearch : TaskListUiEvent()
}
```

### Actions

| Function | Behavior |
|---|---|
| `onCreateTaskClick()` | Emits `NavigateToNewTask` |
| `onTaskClick(task)` | Emits `NavigateToDetail(task.id)` |
| `onSearchClick()` | Emits `NavigateToSearch` |
| `onToggleCompletion(task)` | Calls `repository.toggleTaskCompletion(task)`; sets `errorMessage` on failure |
| `onDeleteTask(task)` | Calls `repository.deleteTask(task)`; sets `errorMessage` on failure |
| `onFilterByTag(tag)` | Updates `activeFilter`; `null` clears the filter |
| `onSortOrderChange(order)` | Updates `activeSortOrder` |
| `onErrorDismissed()` | Clears `errorMessage` |

### In-Memory Filter & Sort Logic

```
Filter: if activeFilter != null → keep tasks where task.tags.contains(activeFilter)
Sort:
  CREATION_DATE → sortedByDescending { creationDate }
  DUE_DATE      → sortedWith(compareBy(nullsLast()) { dueDate })
  PRIORITY      → sortedByDescending { priority.ordinal }  (HIGH first)
```

---

## TaskDetailViewModel

**File:** `ui/taskdetail/TaskDetailViewModel.kt`

Handles both **create** (no `taskId`) and **edit** (with `taskId` from nav argument) in a single ViewModel. On init, if `taskId` is non-null, the task is loaded from the repository and each field is unpacked into `UiState`. A snapshot of the original values is kept privately to compute `isDirty`.

### UiState

```kotlin
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
```

| Field | Notes |
|---|---|
| `isEditMode` | `true` when editing an existing task; `false` for new task creation |
| `isDirty` | `true` when any field differs from the last-saved state; drives the discard dialog |
| `showDiscardDialog` | Controls visibility of the unsaved-changes confirmation dialog |
| `titleError` | Inline validation message shown under the title field; `null` when valid |
| `availableTags` | Full predefined tag list from `repository.getAllTags()`; populated on init |

### UiEvent

```kotlin
sealed class TaskDetailUiEvent {
    object NavigateBack : TaskDetailUiEvent()
}
```

### Actions

| Function | Behavior |
|---|---|
| `onTitleChange(value)` | Updates `title`; recomputes `isDirty`; clears `titleError` |
| `onDescriptionChange(value)` | Updates `description`; recomputes `isDirty` |
| `onDueDateChange(date)` | Updates `dueDate`; recomputes `isDirty` |
| `onPriorityChange(priority)` | Updates `priority`; recomputes `isDirty` |
| `onTagToggle(tag)` | Adds tag if absent, removes if present; recomputes `isDirty` |
| `onSaveClick()` | Validates title (non-blank); calls `insertTask` or `updateTask`; emits `NavigateBack` on success |
| `onBackClick()` | If `isDirty` → sets `showDiscardDialog = true`; else emits `NavigateBack` |
| `onDiscardConfirmed()` | Emits `NavigateBack` (discard dialog confirmed) |
| `onDiscardCancelled()` | Sets `showDiscardDialog = false` (user stayed on screen) |
| `onErrorDismissed()` | Clears `errorMessage` |

### Dirty-State Tracking

`isDirty` is recomputed after every field change by comparing current field values against a private `originalSnapshot: TaskDetailUiState?`. For a new task, `isDirty` is `true` as soon as any field is non-empty/non-default.

---

## SearchViewModel

**File:** `ui/search/SearchViewModel.kt`

### UiState

```kotlin
data class SearchUiState(
    val query: String = "",
    val results: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
```

### UiEvent

```kotlin
sealed class SearchUiEvent {
    data class NavigateToDetail(val taskId: Long) : SearchUiEvent()
    object CopySuccess : SearchUiEvent()       // transient feedback after copy
}
```

### Actions

| Function | Behavior |
|---|---|
| `onQueryChange(query)` | Updates `query`; triggers debounced search |
| `onTaskClick(task)` | Emits `NavigateToDetail(task.id)` |
| `onCopyTask(task)` | Calls `repository.copyAsNewTask(task)`; emits `CopySuccess` on success; sets `errorMessage` on failure |
| `onErrorDismissed()` | Clears `errorMessage` |

### Search Debounce

`onQueryChange` does not call the repository directly. Instead, `query` is written to a `MutableStateFlow<String>`. The ViewModel's `init` block sets up a pipeline:

```
queryFlow
  .debounce(300ms)
  .distinctUntilChanged()
  .flatMapLatest { q -> if (q.isBlank()) flowOf(emptyList()) else repository.searchTasks(q) }
  .collect { results -> update uiState.results }
```

This prevents a database query on every keystroke and cancels in-flight queries when the user keeps typing.
