# SpecTech Android

This folder contains the **design and implementation blueprint** for the Android
version of the SpecTech application, derived from a deep analysis of the
existing iOS app in `../SpecTechIOS`.

The goal is functional parity: every screen, business rule, network call,
domain model, push-notification flow, and persisted piece of state that exists
in iOS must exist in Android. The Android port targets the same backend
(`https://spectech-backoffice.onrender.com/api`), uses the same HMAC request
signing, and produces the same user-facing behavior.

---

## How to read this documentation

The `docs/` folder is split into nine numbered sections. Read them in order
the first time; afterwards each file is self-contained.

| # | Folder | Purpose |
|---|---|---|
| 01 | [overview](docs/01-overview) | What the app does, who uses it, target Android stack |
| 02 | [architecture](docs/02-architecture) | Modules, DI, navigation, state pattern |
| 03 | [domain](docs/03-domain) | Models, enums, value rules — pure Kotlin |
| 04 | [networking](docs/04-networking) | API client, HMAC signing, cert pinning, every endpoint |
| 05 | [features](docs/05-features) | One doc per feature with VM contract + screens |
| 06 | [ui](docs/06-ui) | Theming, design tokens, shared components, i18n |
| 07 | [infrastructure](docs/07-infrastructure) | Keychain ↔ EncryptedDataStore, push, image upload |
| 08 | [testing-build](docs/08-testing-build) | Test strategy, Gradle, signing, Play Console |
| 99 | [appendix](docs/99-appendix) | Endpoint cheat sheet, iOS ↔ Android symbol map |

The [docs/INDEX.md](docs/INDEX.md) file is a one-page table of contents.

---

## Tech stack (target)

- **Language:** Kotlin 2.0+
- **Min SDK:** 26 (Android 8.0) — matches iOS 17+ floor in capability terms
- **Target SDK:** latest stable
- **UI:** Jetpack Compose (Material 3)
- **Architecture:** MVVM with `ViewModel` + `StateFlow` (Android counterpart of iOS `@Observable`)
- **DI:** Hilt
- **Navigation:** Jetpack Navigation Compose (type-safe routes)
- **Concurrency:** Kotlin coroutines + Flow (mirror of iOS `async/await`)
- **HTTP:** Ktor client (engine: OkHttp) — preferred for built-in HMAC interceptor + cert pinning
- **JSON:** kotlinx.serialization
- **Persistence:** EncryptedSharedPreferences / DataStore (Keychain analog) + Room (optional cache)
- **Images:** Coil 2.x (SDWebImageSwiftUI analog)
- **Push:** Firebase Cloud Messaging
- **Maps / city autocomplete:** Google Places SDK (CitySearchField analog)

See [docs/01-overview/02-tech-stack.md](docs/01-overview/02-tech-stack.md) for
the full justification of each choice.

---

## Building (once code lands)

This folder currently contains documentation only. When the Android Studio
project is created next to this README, the conventional layout will be:

```
spectech_android/
├── docs/               ← this documentation
├── app/                ← Android app module
│   ├── build.gradle.kts
│   └── src/main/...
├── core/               ← shared modules: networking, domain, ui-kit
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/
```

See [docs/08-testing-build/02-gradle-setup.md](docs/08-testing-build/02-gradle-setup.md).

---

## Source of truth

When this documentation and the iOS source disagree, **the iOS source wins.**
The iOS app is in production and its behavior is what users expect to keep.
Re-read the relevant Swift file before implementing anything non-trivial.

The iOS source paths referenced throughout these docs are relative to the
repo root (e.g. `SpecTechIOS/Networking/API/APIClient.swift`).
