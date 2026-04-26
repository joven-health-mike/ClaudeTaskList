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

## Theme

`ClaudeTaskListTheme` (in `ui/theme/`) supports dynamic color (Material You) on Android 12+ and falls back to static purple/pink color schemes for older APIs. Dark/light mode follows system setting by default.

## Project Status

This is a freshly scaffolded project. `MainActivity` contains only the default "Hello Android" Compose scaffold — no task-list logic has been implemented yet.
