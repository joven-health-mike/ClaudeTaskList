# ClaudeTaskList — UI Layer Architecture

## Overview

The UI layer is built entirely with **Jetpack Compose** and **Material3**. Composables are pure renderers — they receive `UiState` and emit callbacks; no business logic lives inside them.

Each screen follows this wiring pattern in `MainActivity`'s `NavHost`:

```kotlin
val viewModel: XxxViewModel = hiltViewModel()
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

// One-shot events collected exactly once per screen entry
LaunchedEffect(Unit) {
    viewModel.events.collect { event -> /* call navController */ }
}

XxxScreen(uiState = uiState, onXxx = viewModel::onXxx, ...)
```

---

## Navigation

Declared in `MainActivity` via a single `NavHost`. No bottom navigation bar — the top app bar and FAB are the primary navigation surfaces.

| Route | Screen | Entry point |
|---|---|---|
| `task_list` | `TaskListScreen` | App launch (start destination) |
| `task_detail?taskId={taskId}` | `TaskDetailScreen` | FAB (new) or task tap (edit) |
| `search` | `SearchScreen` | Search icon in top app bar |

`taskId` is an optional nav argument (type `Long`, default `-1`). A value of `-1` signals new-task creation; any positive value signals edit mode.

---

## Screen: TaskListScreen

**File:** `ui/tasklist/TaskListScreen.kt`

### Layout

```
Scaffold
├── TopAppBar
│   ├── Title: "ClaudeTaskList"
│   ├── SearchIconButton  →  NavigateToSearch
│   └── OverflowMenu
│       └── Sort options (Creation Date | Due Date | Priority)
├── content:
│   ├── FilterRow            ← horizontal scrollable tag chip row
│   └── LazyColumn
│       └── SwipeToDismissBox (each item)
│           ├── dismissContent: red delete background with trash icon
│           └── content: TaskItem
└── FloatingActionButton  →  NavigateToNewTask
```

### FilterRow

A horizontally scrollable row of `FilterChip` composables — one per tag plus an "All" chip. The selected chip reflects `uiState.activeFilter` (null = "All" selected).

### TaskItem

```
TaskItem
├── Checkbox          →  onToggleCompletion(task)
├── Column
│   ├── Title         (strikethrough when isCompleted)
│   ├── Tag chips     (small, non-interactive)
│   └── Row: priority badge + due date label
└── onClick           →  onTaskClick(task)
```

Completed tasks remain in the list with a strikethrough title and muted colors. No visual separation from active tasks.

### Swipe to Delete

Implemented with Material3 `SwipeToDismissBox`. Swiping **right-to-left only** reveals a red background with a delete icon once the drag passes the threshold defined in `R.dimen.swipe_to_dismiss_threshold`, then calls `onDeleteTask(task)`. There is no undo at the UI layer — deletion is immediate and permanent (see Repository layer).

### Priority Badge

A small colored `SuggestionChip` or `Text` tag:

| Priority | Color |
|---|---|
| HIGH | `MaterialTheme.colorScheme.error` |
| MEDIUM | `MaterialTheme.colorScheme.tertiary` |
| LOW | `MaterialTheme.colorScheme.secondary` |

---

## Screen: TaskDetailScreen

**File:** `ui/taskdetail/TaskDetailScreen.kt`

Full-screen form for creating and editing tasks.

### Layout

```
Scaffold
├── TopAppBar
│   ├── NavigationIcon: back arrow  →  onBackClick()
│   ├── Title: "New Task" | "Edit Task"
│   └── Actions: "Save" TextButton  →  onSaveClick()
└── content: (vertically scrollable Column)
    ├── OutlinedTextField: Title *        ← required; shows titleError below if invalid
    ├── OutlinedTextField: Description    ← multiline, optional
    ├── DueDateField                      ← read-only field that opens DatePickerDialog on tap
    ├── PrioritySelector                  ← SegmentedButton row (Low | Medium | High)
    └── TagSelector                       ← wrap-row of FilterChip (multi-select)
```

### DueDateField

A non-editable `OutlinedTextField` with a calendar trailing icon. Tapping either the field or the icon opens a Material3 `DatePickerDialog`. A clear (×) icon appears when a date is set.

### PrioritySelector

A Material3 `SingleChoiceSegmentedButtonRow` with three segments: Low, Medium, High. Reflects `uiState.priority`; calls `onPriorityChange` on selection.

### TagSelector

A `FlowRow` of `FilterChip` composables built from `uiState.availableTags`. Selected state driven by `uiState.selectedTags`.

### Discard Changes Dialog

Shown when `uiState.showDiscardDialog == true`:

```
AlertDialog
├── title: "Discard changes?"
├── text:  "You have unsaved changes. Leave without saving?"
├── confirmButton: "Discard"  →  onDiscardConfirmed()
└── dismissButton: "Keep editing"  →  onDiscardCancelled()
```

### Validation

`titleError` is displayed as a `supportingText` on the title `OutlinedTextField`. The Save button is always enabled — validation runs on tap, not on each keystroke.

---

## Screen: SearchScreen

**File:** `ui/search/SearchScreen.kt`

### Layout

```
Scaffold
├── TopAppBar
│   ├── NavigationIcon: back arrow  →  navigates back via NavController
│   └── SearchBar (fills remaining width)  →  onQueryChange(query)
└── content:
    └── LazyColumn
        └── SearchResultItem  (per result)
```

The `SearchBar` is focused automatically on screen entry via `LaunchedEffect`.

### SearchResultItem

A `TaskItem` variant with an additional **Copy** icon button on the trailing edge. Tapping it calls `onCopyTask(task)`. The item itself is still tappable to navigate to the detail screen for viewing/editing.

```
SearchResultItem
├── Checkbox          (same toggle behavior as TaskItem)
├── Column: title, tags, priority + due date
├── CopyIconButton    →  onCopyTask(task)   ← only shown for completed tasks
└── onClick           →  onTaskClick(task)
```

A `Snackbar` is shown transiently when `uiState.copySuccessMessage` is non-null, confirming the task was added to the active list.

---

## Shared Composables

**Package:** `ui/components/`

| Composable | Description |
|---|---|
| `TaskItem` | Reusable task row used in `TaskListScreen`; extended by `SearchResultItem` |
| `PriorityBadge` | Colored chip/label for Low / Medium / High |
| `TagChip` | Small non-interactive chip for displaying a tag name on a task item |
| `DiscardChangesDialog` | The unsaved-changes `AlertDialog` (shared if reused across screens) |

---

## Theme

`ClaudeTaskListTheme` (in `ui/theme/`) wraps all screens. It supports:
- **Dynamic color** (Material You) on Android 12+ — adapts to the user's wallpaper
- **Static fallback** — purple/pink color scheme on Android 11 and below
- **Dark/light mode** — follows system setting automatically

No custom theme tokens are needed beyond what Material3 provides out of the box.

---

## Composable Conventions

- Every screen composable is `@Preview`-able with a hardcoded `UiState` — no ViewModel reference inside composables
- Lambdas are named to match the ViewModel's action functions (`onSaveClick`, `onToggleCompletion`, etc.)
- State hoisting stops at the screen level — sub-composables receive only the slice of state they need
- `collectAsStateWithLifecycle()` is used instead of `collectAsState()` to respect the lifecycle and avoid updates while the app is in the background
