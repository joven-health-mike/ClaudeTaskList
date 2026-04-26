# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests (JVM, no device needed)
./gradlew test

# Run a single unit test class
./gradlew test --tests "com.lordinatec.claudetasklist.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Clean build
./gradlew clean
```

## Architecture & Stack

- **Single-module Android app** (`app/`), package `com.lordinatec.claudetasklist`
- **Kotlin + Jetpack Compose** with Material3; no XML layouts
- **Min SDK 34, Target SDK 36** (Android 14+); requires physical device or emulator running API 34+
- **Version catalog** at `gradle/libs.versions.toml` — all dependency versions and plugin aliases are defined there; reference them via `libs.*` in `build.gradle.kts`
- **Pattern:** MVVM with Hilt DI and a Repository layer over Room

Full architecture documentation is in `architecture/`:

| Document | Covers |
|---|---|
| [`ARCHITECTURE.md`](architecture/ARCHITECTURE.md) | High-level layer diagram, data model overview, navigation routes, Hilt module layout |
| [`MODEL.md`](architecture/MODEL.md) | Domain models, Room entities, DAOs, type storage, mapper pattern, seeding strategy |
| [`REPOSITORY.md`](architecture/REPOSITORY.md) | `TaskRepository` interface, all read/write method contracts, `Result<T>` error handling, business invariants |
| [`VIEWMODEL.md`](architecture/VIEWMODEL.md) | Per-screen `UiState` / `UiEvent` definitions, action functions, filter/sort pipeline, search debounce |
| [`UI.md`](architecture/UI.md) | Screen layouts, composable hierarchy, navigation wiring, shared components, theme |

## Project Status

The application is fully implemented. All layers — Room database, Repository, ViewModels, and Compose UI screens — are complete and wired together. Unit tests exist for all three ViewModels (`TaskListViewModelTest`, `TaskDetailViewModelTest`, `SearchViewModelTest`).
