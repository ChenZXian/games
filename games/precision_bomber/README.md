# Precision Bomber

Android mini-game (Gradle project) located under `games/precision_bomber/`.

## Prerequisites

- Windows + PowerShell
- Android SDK installed
- JDK 21

## Validate (repo standard)

From repo root:

```powershell
powershell -ExecutionPolicy Bypass -File tools/validate.ps1 -Project games/precision_bomber
```

## Build APK (repo standard)

From repo root:

```powershell
powershell -ExecutionPolicy Bypass -File tools/build_apk.ps1 -Project games/precision_bomber -Variant debug
```

If successful, the final APK will be exported under:

- `artifacts/apk/precision_bomber/`

## Open in Android Studio

Open the folder:

- `games/precision_bomber/`


