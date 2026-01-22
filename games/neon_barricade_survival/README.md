# Neon Barricade Survival

Android mini-game (Gradle project) located under `games/neon_barricade_survival/`.

## Prerequisites

- Windows + PowerShell
- Android SDK installed
- JDK 17

## Validate (repo standard)

From repo root:

```powershell
powershell -ExecutionPolicy Bypass -File tools/validate.ps1 -Project games/neon_barricade_survival
```

## Build APK (repo standard)

From repo root:

```powershell
powershell -ExecutionPolicy Bypass -File tools/build_apk.ps1 -Project games/neon_barricade_survival -Variant debug
```

If successful, the final APK will be exported under:

- `artifacts/apk/neon_barricade_survival/`

## Open in Android Studio

Open the folder:

- `games/neon_barricade_survival/`


