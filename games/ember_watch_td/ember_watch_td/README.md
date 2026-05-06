# Ember Watch TD

Ember Watch TD is a Java Android route defense prototype built as a reusable foundation for a larger commercial project.

## Highlights

- Four core tower lines
- Base upgrades and branch upgrades
- Ground and flying enemies
- Armor and magic resist
- Barracks blocking soldiers
- Two active skills
- One controllable hero
- One complete stage with five waves

## Run

1. Open this folder in Android Studio
2. Ensure Android SDK 34 is installed
3. Ensure Gradle uses JDK 21 or JDK 17
4. Provide gradle-wrapper.jar if your environment requires the wrapper script
5. Sync and run the app

## Key Package Layout

- com.android.boot
- com.android.boot.core
- com.android.boot.ui
- com.android.boot.entity
- com.android.boot.data

## Extend

- Add more entries in GameDatabase for enemies and waves
- Add new tower branches in GameSession
- Add new levels by creating more LevelConfig instances
- Add new skills by expanding Skill and GameSession.triggerSkill
