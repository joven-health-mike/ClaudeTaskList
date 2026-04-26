# ClaudeTaskList

A task list application for Android built with Kotlin and Jetpack Compose.

## Features

### Tasks
- Create, edit, and delete tasks
- Mark tasks complete or incomplete; completed tasks remain in the main list with a strikethrough/checkmark
- Each task holds:
  - **Title** (required)
  - **Description/notes** (optional)
  - **Due date** (optional)
  - **Priority** — Low, Medium, or High
  - **Creation date** (auto-set)
  - **Tags** — one or more predefined categories per task

### Tags
Predefined categories for organizing tasks:
- Work
- Finances
- Health
- Personal
- Shopping
- Other

### Browse & Search
- Filter the task list by tag
- Sort tasks by creation date, due date, or priority
- Search across all tasks (active and completed) by title or description
- Copy any completed task as a new active task — the completed original is preserved

### Persistence
- All tasks are stored locally using [Room](https://developer.android.com/training/data-storage/room)

## Planned / Future Features
- User-managed tags (create, rename, delete custom tags)
- Recurring tasks (daily, weekly, etc.)
- Due date notifications and reminders

## Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material3
- **Database:** Room
- **Min SDK:** 34 (Android 14)

## Building

```bash
./gradlew assembleDebug
```

Run unit tests:
```bash
./gradlew test
```
