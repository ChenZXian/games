# Mosaic Studio

Android mini-game (Gradle project) located under `games/mosaic_studio/`.

## Prerequisites

- Windows + PowerShell
- Android SDK installed
- JDK 17

## Validate (repo standard)

From repo root:

```powershell
powershell -ExecutionPolicy Bypass -File tools/validate.ps1 -Project games/mosaic_studio
```

## Build APK (repo standard)

From repo root:

```powershell
powershell -ExecutionPolicy Bypass -File tools/build_apk.ps1 -Project games/mosaic_studio -Variant debug
```

If successful, the final APK will be exported under:

- `artifacts/apk/mosaic_studio/`

## Open in Android Studio

Open the folder:

- `games/mosaic_studio/`


