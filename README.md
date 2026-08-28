# Focus

A local-first Android countdown calendar. Each day in an inclusive goal range is shown as a contribution-style cell whose intensity reflects completed tasks.

## Current slice

- Kotlin + Jetpack Compose + Material 3
- One editable goal configured on first launch
- Goal editing for the title, start date, and end date
- Local task creation, completion, and deletion
- Seven-column Sunday-to-Saturday calendar grid with one row per week
- Inclusive date range and deterministic intensity calculation
- SharedPreferences persistence

## Build

Open this folder in Android Studio with an Android SDK installed, then run the `app` configuration. The project targets API 35 and requires Java 21. The lock-screen widget and notification projection are the next implementation slice.

## Intensity

A day with no tasks is neutral. Otherwise, completion is mapped to low (<33%), medium (33-65%), high (66-99%), or full (100%).
