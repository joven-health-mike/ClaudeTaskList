# ClaudeTaskList — Architecture

## Pattern: MVVM

The app follows the Model-View-ViewModel (MVVM) pattern as recommended by Google for Android + Jetpack Compose. Data flows unidirectionally: the UI observes state emitted by ViewModels; user actions are dispatched back up as events.

```
┌─────────────────────────────────────┐
│              UI Layer               │
│   Composables observe StateFlow     │
│   and dispatch events to ViewModel  │
└────────────────┬────────────────────┘
                 │ events / state
┌────────────────▼────────────────────┐
│           ViewModel Layer           │
│   Holds UI state as StateFlow       │
│   Calls Repository (suspend/Flow)   │
└────────────────┬────────────────────┘
                 │ suspend / Flow
┌────────────────▼────────────────────┐
│          Repository Layer           │
│   Single source of truth for data   │
│   Abstracts Room DAOs from VMs      │
└────────────────┬────────────────────┘
                 │ DAO calls
┌────────────────▼────────────────────┐
│            Data Layer               │
│   Room database, DAOs, Entities     │
└─────────────────────────────────────┘
```

Dependency injection is handled by **Hilt** throughout all layers.

---

## Layers

### UI Layer (`ui/`)
- Built entirely with **Jetpack Compose** and **Material3**
- Each screen is a `@Composable` function that takes state and a lambda/callback for events
- No business logic lives in composables — they are pure renderers of `UiState`
- Navigation is handled by **Compose Navigation** (`NavHost`) in `MainActivity`

### ViewModel Layer (`ui/<feature>/`)
- One `ViewModel` per screen, scoped to the `NavBackStackEntry`
- Exposes a single `uiState: StateFlow<UiState>` per screen
- Receives user actions via plain functions (not sealed classes, unless MVI is adopted later)
- Injected by Hilt via `@HiltViewModel`

### Repository Layer (`data/repository/`)
- One repository: `TaskRepository`
- Exposes `Flow<List<Task>>` for reactive list queries and `suspend` functions for writes
- Responsible for mapping between database `Entity` objects and domain `Task`/`Tag` models

### Data Layer (`data/`)
- **Room** database (`TaskDatabase`) with the following tables:
  - `tasks` — stores task fields
  - `tags` — stores predefined tag definitions
  - `task_tags` — cross-reference table for the many-to-many task↔tag relationship
- DAOs: `TaskDao`, `TagDao`

---

## Data Model

```
Task
├── id: Long (PK, auto-generated)
├── title: String
├── description: String?
├── dueDate: LocalDate?
├── priority: Priority (LOW | MEDIUM | HIGH)
├── creationDate: LocalDate (auto-set on insert)
└── isCompleted: Boolean

Tag
├── id: Long (PK, auto-generated)
└── name: String  ← predefined values seeded on first launch

TaskTagCrossRef  (many-to-many join)
├── taskId: Long (FK → Task)
└── tagId: Long  (FK → Tag)
```

**Predefined tags** (seeded at database creation): Work, Finances, Health, Personal, Shopping, Other

---

## Navigation

Single-activity app (`MainActivity`). All navigation is handled by a `NavHost` in `MainActivity`.

| Route | Screen | Description |
|---|---|---|
| `task_list` | `TaskListScreen` | Main screen — all tasks, filter/sort controls |
| `task_detail/{taskId}` | `TaskDetailScreen` | Create or edit a task |
| `search` | `SearchScreen` | Search across all tasks; copy completed tasks |

---

## Screens & ViewModels

| Screen | ViewModel | Responsibilities |
|---|---|---|
| `TaskListScreen` | `TaskListViewModel` | Load tasks, apply filter/sort, toggle completion, delete |
| `TaskDetailScreen` | `TaskDetailViewModel` | Load task by ID (or blank for new), save, validate |
| `SearchScreen` | `SearchViewModel` | Query tasks by keyword, copy completed task as new active task |

---

## Dependency Injection (Hilt)

```
AppModule
└── provides: TaskRepository

DatabaseModule
├── provides: TaskDatabase (singleton)
├── provides: TaskDao
└── provides: TagDao
```

`@HiltAndroidApp` on `ClaudeTaskListApplication`  
`@AndroidEntryPoint` on `MainActivity`  
`@HiltViewModel` on each ViewModel

---

## Key Conventions

- **Entities** (`*Entity`) live in `data/model/` and are Room-annotated
- **Domain models** (`Task`, `Tag`) live in `domain/model/` — no Android/Room imports
- **UiState** data classes are defined in the same file as their ViewModel
- All database operations run on `Dispatchers.IO` (enforced in the Repository)
- Tags are seeded via a `RoomDatabase.Callback` on first creation
