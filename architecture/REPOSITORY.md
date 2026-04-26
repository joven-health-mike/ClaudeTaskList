# ClaudeTaskList — Repository Layer Architecture

## Overview

The repository layer has a single class: `TaskRepository`. It is the only component that touches the data layer — no ViewModel or domain code ever calls a DAO directly.

```
ViewModel  ──►  TaskRepository  ──►  TaskDao / TagDao  ──►  Room
               (data/repository/)     (data/dao/)
```

The repository:
- Enforces all business invariants on domain data before writing (see [Invariants](#invariants))
- Maps between Room entities and domain models using mapper extension functions
- Runs all database work on `Dispatchers.IO` via `withContext`
- Returns `Result<T>` for all write operations; `Flow` for all reads

---

## Interface

```kotlin
interface TaskRepository {
    // Reads — reactive, no Result wrapper
    fun getAllTasks(): Flow<List<Task>>
    fun getTaskById(id: Long): Flow<Task?>
    fun searchTasks(query: String): Flow<List<Task>>
    fun getAllTags(): Flow<List<Tag>>

    // Writes — suspend, wrapped in Result
    suspend fun insertTask(task: Task): Result<Long>
    suspend fun updateTask(task: Task): Result<Unit>
    suspend fun deleteTask(task: Task): Result<Unit>
    suspend fun toggleTaskCompletion(task: Task): Result<Unit>
    suspend fun copyAsNewTask(task: Task): Result<Long>
}
```

A concrete `TaskRepositoryImpl` in the same package implements this interface and is bound in the Hilt `AppModule`.

---

## Read Operations

Reads return `Flow` directly — no `Result` wrapper. Downstream errors are handled by the standard Flow `catch` operator in the ViewModel.

| Method | DAO call | Notes |
|---|---|---|
| `getAllTasks()` | `TaskDao.getAllTasksWithTags()` | Full list; filtering and sorting done in ViewModel |
| `getTaskById(id)` | `TaskDao.getTaskWithTagsById(id)` | Emits `null` if the task doesn't exist |
| `searchTasks(query)` | `TaskDao.searchTasksWithTags(query)` | Matches on `title` and `description` |
| `getAllTags()` | `TagDao.getAll()` | Predefined tag list for UI pickers |

---

## Write Operations

All write operations are `suspend` functions that return `Result<T>`. They wrap their body in a `runCatching` block so any Room exception becomes a `Result.failure` — the ViewModel never needs `try/catch`.

### `insertTask(task: Task): Result<Long>`
- Sets `creationDate` to `LocalDate.now()` (ignores any value on the incoming `Task`)
- Inserts the `TaskEntity`, then inserts one `TaskTagCrossRef` row per tag
- Returns `Result<Long>` containing the new task's auto-generated ID

### `updateTask(task: Task): Result<Unit>`
- Does **not** modify `creationDate`
- Deletes all existing `TaskTagCrossRef` rows for this task, then re-inserts the current tag set (replace strategy)
- Does **not** touch `isCompleted` or `completedDate` — use `toggleTaskCompletion` for that

### `deleteTask(task: Task): Result<Unit>`
- Hard-deletes the `TaskEntity`; Room `CASCADE` removes cross-ref rows automatically

### `toggleTaskCompletion(task: Task): Result<Unit>`
- If `isCompleted` is currently `false`: sets `isCompleted = true` and `completedDate = LocalDate.now()`
- If `isCompleted` is currently `true`: sets `isCompleted = false` and `completedDate = null`
- Calls `updateTask` internally after applying the change

### `copyAsNewTask(task: Task): Result<Long>`
Copies content from a completed task into a brand-new active task. The original task is unchanged.

The new task is constructed as:
```
title        = source.title
description  = source.description
dueDate      = source.dueDate
priority     = source.priority
tags         = source.tags
id           = 0               ← signals a new (un-persisted) task
isCompleted  = false
completedDate = null
creationDate  = (set to today by insertTask)
```
Delegates to `insertTask` to persist the new task.

---

## Invariants

The repository is the single enforcement point for these rules. No caller needs to know about them.

| Invariant | Enforced in |
|---|---|
| `creationDate` is always `LocalDate.now()` on insert, never changed on update | `insertTask` |
| `completedDate` is set to today when completing, cleared to `null` when uncompleting | `toggleTaskCompletion` |
| Tag updates always delete-then-reinsert (no partial patch) | `insertTask`, `updateTask` |
| A copy of a completed task is always inserted as a fresh incomplete task | `copyAsNewTask` |

---

## Error Handling

```kotlin
// Inside TaskRepositoryImpl — all writes follow this pattern
override suspend fun insertTask(task: Task): Result<Long> = runCatching {
    withContext(Dispatchers.IO) {
        val entity = task.copy(creationDate = LocalDate.now()).toEntity()
        val id = taskDao.insert(entity)
        task.tags.forEach { tag ->
            taskDao.insertTaskTagCrossRef(TaskTagCrossRef(id, tag.id))
        }
        id
    }
}
```

The ViewModel handles the result with `onSuccess` / `onFailure` — no `try/catch` needed at the call site.

---

## Hilt Wiring

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        tagDao: TagDao
    ): TaskRepository = TaskRepositoryImpl(taskDao, tagDao)
}
```

---

## Filtering & Sorting

Filtering by tag and sorting by due date or priority are **not** done in the repository. `getAllTasks()` always emits the complete task list. The ViewModel applies filter and sort criteria in-memory using Kotlin collection operations on the emitted `List<Task>`.

This keeps the DAO queries simple and avoids duplicating query variants for every filter/sort combination.
