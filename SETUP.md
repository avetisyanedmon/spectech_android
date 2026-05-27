# Setup

This is the first-time setup for `spectech_android/`. Run through these steps
once on any machine that needs to build the app.

## 1. Prerequisites

| Tool | Required version | Check |
|---|---|---|
| **JDK** | **17 or 21** (LTS) | `java -version` |
| **Android Studio** | Ladybug or newer (AGP 8.7+) | open it once |
| **Android SDK** | Platform 35, Build-Tools 35.x | installed via Android Studio SDK Manager |

> ⚠️ Your machine currently has JDK 22 and no Android SDK. AGP 8.7 supports
> JDK 17 and 21 — install one of those before building. Easiest path on macOS:
>
> ```bash
> brew install --cask temurin@21
> # then in IntelliJ/Android Studio: Project Structure → SDK Location → JDK 21
> ```

## 2. Install Android Studio + SDK

1. Download Android Studio: https://developer.android.com/studio
2. On first launch, the setup wizard will install the SDK at
   `~/Library/Android/sdk`.
3. Open SDK Manager (Tools → SDK Manager) and install:
   - **Android 15 (API 35)** SDK Platform
   - **Android SDK Build-Tools 35.0.0**
   - **Android SDK Platform-Tools** (latest)

## 3. Open the project

```bash
open -a "Android Studio" /Users/edmon/Documents/spectech_ios/spectech_android
```

Or from inside Android Studio: **File → Open** → select the `spectech_android`
folder.

On first sync Android Studio will:
- Download the Gradle wrapper (8.11.1) — this populates `gradle/wrapper/gradle-wrapper.jar`
- Download all dependencies (Compose BOM, Hilt, Ktor, Coil, etc.)
- Index the project

This takes ~5 minutes the first time.

## 4. Build & run

```bash
# Sync Gradle
./gradlew assembleDebug          # APK at app/build/outputs/apk/debug/

# Or via Android Studio: Run → Run 'app'
```

To run on a device:
1. Connect a physical device with USB debugging on, or start an emulator
   from Device Manager.
2. Press the green ▶ button in the toolbar (or `Ctrl-R`).

You should see **Hello SpecTech** centered on a Material 3 surface.

## 5. (Later) Things to install before Phase 11

These aren't needed for Phase 0 but will be required as features land:

- **Firebase project** — drop `google-services.json` into `app/` for push
  notifications (Phase 11). See [docs/07-infrastructure/02-push-notifications.md](docs/07-infrastructure/02-push-notifications.md).
- **Google Places API key** — for city autocomplete (Phase 6). Add to
  `local.properties` as `placesApiKey=…` and reference from manifest.
- **Release keystore** — generate before first release. See [docs/08-testing-build/04-release.md](docs/08-testing-build/04-release.md).

## 6. Verifying the scaffolding

The first build should:
- Resolve all dependencies (no red text in Gradle sync)
- Compile every module (16 of them)
- Produce a runnable APK
- Launch on an emulator/device showing "Hello SpecTech"

If Gradle sync fails, the most common causes are:
- Wrong JDK (must be 17 or 21, NOT 22)
- Missing Android SDK (install via SDK Manager)
- Missing Android 15 platform (install via SDK Manager)

## What's NOT in this scaffolding

Per the [step-by-step build plan](docs/99-appendix/04-step-by-step-build-plan.md),
Phase 0 covers the multi-module Gradle skeleton and a Hello-screen `:app`.
The 15 library modules contain only `Placeholder.kt` files and will be
filled in over Phases 1–17.

Next phase: [Phase 1 — Domain & Network](docs/99-appendix/04-step-by-step-build-plan.md#phase-1--domain--network-2-days).
