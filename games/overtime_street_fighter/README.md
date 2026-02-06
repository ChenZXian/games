# Overtime Street Fighter

Android mini-game (Gradle project) located under `games/overtime_street_fighter/`.

## Prerequisites

- Windows + PowerShell
- Android SDK installed
- JDK 21

## Validate (repo standard)

From repo root:

```powershell
powershell -ExecutionPolicy Bypass -File tools/validate.ps1 -Project games/overtime_street_fighter
```

## Build APK (repo standard)

From repo root:

```powershell
powershell -ExecutionPolicy Bypass -File tools/build_apk.ps1 -Project games/overtime_street_fighter -Variant debug
```

If successful, the final APK will be exported under:

- `artifacts/apk/overtime_street_fighter/`

## Open in Android Studio

Open the folder:

- `games/overtime_street_fighter/`

