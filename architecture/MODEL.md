# ClaudeTaskList — Model Layer Architecture

## Overview

The model layer is split into two distinct representations:

| Layer | Location | Purpose |
|---|---|---|
| **Domain models** | `domain/model/` | Pure Kotlin data classes; no Android or Room imports |
| **Room entities** | `data/model/` | Room-annotated classes; mapped to/from domain models |

ViewModels and the Repository always work with **domain models**. Room entities never leave the data layer.

---

## Domain Models

### `Task`

```kotlin
data class Task(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val dueDate: LocalDate? = null,
    val priority: Priority = Priority.MEDIUM,
    val creationDate: LocalDate,
    val completedDate: LocalDate? = null,
    val isCompleted: Boolean = false,
    val tags: List<Tag> = emptyList()
)
```

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | 0 signals a new (un-persisted) task |
| `title` | `String` | Required; non-blank enforced in ViewModel |
| `description` | `String?` | Optional free-text notes |
| `dueDate` | `LocalDate?` | Date only — no time component |
| `priority` | `Priority` | Defaults to `MEDIUM` on creation |
| `creationDate` | `LocalDate` | Set by the Repository on first insert, never updated |
| `completedDate` | `LocalDate?` | Set to today when `isCompleted` flips to `true`; cleared when flipped back |
| `isCompleted` | `Boolean` | Toggling to `true` sets `completedDate`; toggling to `false` clears it |
| `tags` | `List<Tag>` | Resolved from the join table; empty list if untagged |

### `Tag`

```kotlin
data class Tag(
    val id: Long,
    val name: String
)
```

### `Priority`

```kotlin
enum class Priority {
    LOW, MEDIUM, HIGH
}
```

---

## Room Entities

### `TaskEntity`

```kotlin
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String?,
    val dueDate: Long?,          // epoch day via LocalDate.toEpochDay()
    val priority: String,        // Priority.name (e.g. "MEDIUM")
    val creationDate: Long,      // epoch day
    val completedDate: Long?,    // epoch day; null when incomplete
    val isCompleted: Boolean
)
```

### `TagEntity`

```kotlin
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
```

### `TaskTagCrossRef`

```kotlin
@Entity(
    tableName = "task_tags",
    primaryKeys = ["taskId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = CASCADE),
        ForeignKey(entity = TagEntity::class,  parentColumns = ["id"], childColumns = ["tagId"],  onDelete = CASCADE)
    ]
)
data class TaskTagCrossRef(
    val taskId: Long,
    val tagId: Long
)
```

`onDelete = CASCADE` on `taskId` ensures cross-ref rows are removed automatically when a task is hard-deleted.

### `TaskWithTags` (Room relation — not a table)

```kotlin
data class TaskWithTags(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(TaskTagCrossRef::class, parentColumn = "taskId", entityColumn = "tagId")
    )
    val tags: List<TagEntity>
)
```

Room populates this automatically when the DAO query returns a `TaskWithTags`.

---

## Type Storage

`LocalDate` is stored as a **Long epoch day** (`LocalDate.toEpochDay()` / `LocalDate.ofEpochDay()`).  
`Priority` is stored as its **String name** (`Priority.name` / `Priority.valueOf()`).

There is no `@TypeConverters` class. The entity fields are raw `Long` and `String` columns; conversions happen explicitly in the mapper extension functions (`data/mapper/TaskMapper.kt`), keeping the schema transparent and the conversion logic co-located with the mapping logic.

---

## DAOs

### `TaskDao`

| Method | Return | Notes |
|---|---|---|
| `insert(task: TaskEntity): Long` | `suspend` | Returns the new row ID |
| `update(task: TaskEntity)` | `suspend` | Full-row replace by primary key |
| `delete(task: TaskEntity)` | `suspend` | Hard delete |
| `getTaskWithTagsById(id: Long): Flow<TaskWithTags?>` | `Flow` | Null if not found |
| `getAllTasksWithTags(): Flow<List<TaskWithTags>>` | `Flow` | All tasks, unsorted (sorting in Repository/ViewModel) |
| `searchTasksWithTags(query: String): Flow<List<TaskWithTags>>` | `Flow` | `LIKE '%query%'` on `title` and `description` |
| `insertTaskTagCrossRef(crossRef: TaskTagCrossRef)` | `suspend` | Adds one tag to a task |
| `deleteTaskTagCrossRefsForTask(taskId: Long)` | `suspend` | Removes all tags from a task before re-inserting |

### `TagDao`

| Method | Return | Notes |
|---|---|---|
| `getAll(): Flow<List<TagEntity>>` | `Flow` | All predefined tags |
| `insert(tag: TagEntity): Long` | `suspend` | Used only during seeding |

---

## Database

```kotlin
@Database(
    entities = [TaskEntity::class, TagEntity::class, TaskTagCrossRef::class],
    version = 1,
    exportSchema = true
)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun tagDao(): TagDao
}
```

### Seeding

Predefined tags are inserted via a `RoomDatabase.Callback.onCreate` callback provided in the Hilt `DatabaseModule`. This runs exactly once when the database file is first created.

```
Predefined tags: Work, Finances, Health, Personal, Shopping, Other
```

---

## Mapper Pattern

Extension functions in `data/mapper/` convert between entity and domain representations. The Repository calls these; nothing outside the data layer ever touches an entity.

```kotlin
// TaskEntity + List<TagEntity> → Task
fun TaskWithTags.toDomain(): Task

// Task → TaskEntity  (tags handled separately via cross-ref table)
fun Task.toEntity(): TaskEntity

// TagEntity → Tag
fun TagEntity.toDomain(): Tag
```

---

## Deletion Behavior

Tasks are **hard-deleted**. When `TaskDao.delete()` is called:
- The `tasks` row is removed
- `task_tags` cross-ref rows for that task are removed automatically via `CASCADE`

There is no soft-delete, trash, or undo at the data layer. Undo (if desired in the UI) must be handled transiently in the ViewModel before any database call is made.

---

## Invariants Enforced by the Repository

- `creationDate` is always set to `LocalDate.now()` on insert and never modified on update
- When `isCompleted` is set to `true`, `completedDate` is set to `LocalDate.now()`
- When `isCompleted` is set to `false`, `completedDate` is cleared to `null`
- Tag updates for a task always delete all existing cross-refs then re-insert the new set (replace strategy)
