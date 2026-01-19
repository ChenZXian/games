# Sky Relic Runner Shift

Android mini-game (Gradle project) located under `games/sky_relic_runner_shift/`.

## Prerequisites

- Windows + PowerShell
- Android SDK installed
- JDK 17

## Validate (repo standard)

From repo root:

```powershell
powershell -ExecutionPolicy Bypass -File tools/validate.ps1 -Project games/sky_relic_runner_shift
```

## Build APK (repo standard)

From repo root:

```powershell
powershell -ExecutionPolicy Bypass -File tools/build_apk.ps1 -Project games/sky_relic_runner_shift -Variant debug
```

If successful, the final APK will be exported under:

- `artifacts/apk/sky_relic_runner_shift/`

## Open in Android Studio

Open the folder:

- `games/sky_relic_runner_shift/`


