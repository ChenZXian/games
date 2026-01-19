- # Environment Baseline (Single Source of Truth)

  > Project: Orbit Dodger  
  > Baseline Version: v1  
  > Last Updated: 2026-01-18  
  > Status: Verified (core toolchain locked), SDK packages pending doctor verification

  ---

  ## 1) OS / Shell

  - OS: Windows 10/11 (Windows_NT)
  - Shell: PowerShell 5.1+

  ---

  ## 2) Java

  ### Build JDK Policy
  - Recommended JDK: 21
  - Allowed JDK: 17 or 21
  - Repo standard: JDK 21

  ### How Gradle Must Use Java
  - Required: Gradle must use JDK via `org.gradle.java.home`
  - Location: `gradle.properties`
  - Example:
    - `org.gradle.java.home=D:\\Java\\jdk-21`

  ### Notes
  - Android Studio ships with its own runtime (JBR). Do not assume Studio runtime equals Gradle runtime.
  - If system default Java is not 17/21, builds must still succeed because Gradle is pinned to `org.gradle.java.home`.

  ---

  ## 3) Android Toolchain

  ### Android Studio
  - Android Studio: Otter 2 Feature Drop | 2025.2.2 Patch 1
  - Build: AI-252.27397.103.2522.14617522 (built on Dec 19, 2025)
  - Runtime (JBR): 21.0.8+ (JetBrains Runtime amd64)

  ### Gradle / AGP
  - Android Gradle Plugin (AGP): 8.13.2 (locked)
    - Must be declared via Plugins DSL only:
      - `plugins { id "com.android.application" version "8.13.2" apply false }`
    - Rule: Do not re-declare AGP via `buildscript { classpath ... }` in any module.
  - Gradle Wrapper: 8.14.3 (locked)
    - Must be locked in: `gradle/wrapper/gradle-wrapper.properties`
    - distributionUrl must reference `gradle-8.14.3-bin.zip`

  ### Language
  - Kotlin: Not used (Java-only project)

  ---

  ## 4) Android SDK / Build

  ### SDK Levels
  - compileSdk: 34
  - targetSdk: 34
  - minSdk: 24

  ### Build Tools
  - buildTools: Not pinned (AGP default)
  - Requirement: Build-Tools 34.x must be installed on developer machines / CI runners
    - Recommended: 34.0.0
  - Rule: Do not set `buildToolsVersion` unless a specific reproducibility issue is found.

  ### NDK
  - Not used

  ---

  ## 5) Android SDK Location Policy

  - Preferred: `local.properties` exists and contains:
    - `sdk.dir=<path to Android SDK>`
  - Alternative: set environment variable:
    - `ANDROID_SDK_ROOT=<path to Android SDK>`
  - Rule: At least one of the above must be present for builds and install tasks.

  ---

  ## 6) Required SDK Packages (Baseline)

  These packages must be available (installed) for build and install:

  - platforms;android-34
  - build-tools;34.0.0 (or any 34.x)
  - platform-tools

  Optional (only if emulator is used):
  - emulator
  - system-images;android-34;google_apis;x86_64 (or equivalent)

  ---

  ## 7) Project Build Outputs

  - Default build output: APK
  - Variants:
    - debug: must pass (validated)
    - release: should pass assembleRelease (signing may be external)

  Expected debug APK path:
  - `app/build/outputs/apk/debug/app-debug.apk`

  ---

  ## 8) Gradle Properties Baseline

  `gradle.properties` must contain at least:

  - `android.useAndroidX=true`
  - `android.enableJetifier=true`
  - `org.gradle.jvmargs=-Xmx2048m`
  - `org.gradle.java.home=<JDK 21 path>`

  Example:

  ```properties
  android.useAndroidX=true
  android.enableJetifier=true
  org.gradle.jvmargs=-Xmx2048m
  org.gradle.java.home=D\:\\Java\\jdk-21
  ```

## 9) Dependencies Baseline

- Material Components:
  - `com.google.android.material:material:1.11.0`

AndroidX configuration:

- android.useAndroidX=true
- android.enableJetifier=true

------

## 10) Release Policy

- Requirement: `assembleRelease` should succeed
- Signing:
  - Debug uses debug keystore
  - Release signing is allowed to be handled outside repo (CI or local), unless explicitly added later

------

## 11) Repo Enforcement Rules

- Gradle wrapper locked: Yes
  - `gradlew` / `gradlew.bat` present
  - `gradle-wrapper.jar` present
  - `gradle-wrapper.properties` present
- AGP locked via plugins DSL: Yes
- Version catalog (libs.versions.toml): Not used
- Template sync:
  - Recommended: Yes (new game templates must match this baseline)
  - Current status: Pending enforcement scripts

------

## 12) Verification Checklist

Core toolchain:

-  Android Studio version pinned (Otter 2 Feature Drop 2025.2.2 Patch 1)
-  AGP pinned (8.13.2)
-  Gradle wrapper pinned (8.14.3)
-  compileSdk/targetSdk/minSdk defined (34/34/24)
-  Gradle uses JDK via `org.gradle.java.home` (JDK 21)

SDK sanity (to be verified by doctor script):

-  platforms;android-34 installed
-  build-tools 34.x installed
-  platform-tools installed (adb available)
-  SDK location policy satisfied (local.properties or ANDROID_SDK_ROOT)

Release:

-  assembleRelease verified (signing optional)

------

## 13) Quick Commands

Build debug APK (Windows):

```
.\gradlew.bat clean assembleDebug
```

Install debug to device (Windows):

```
.\gradlew.bat installDebug
```

Check Gradle version:

```
.\gradlew.bat -v
```

Clean:

```
.\gradlew.bat clean
```