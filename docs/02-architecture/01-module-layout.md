# 01 — Module Layout

A multi-module Gradle layout keeps build times low and enforces the same
boundaries iOS achieves through its folder structure
(`Features/`, `Networking/`, `Infrastructure/`).

```
spectech_android/
├── app/                           ← Android entry point only
│   └── src/main/kotlin/ru/spectech/android/
│       ├── SpecTechApplication.kt
│       └── MainActivity.kt
├── core/
│   ├── domain/                    ← pure Kotlin (no Android deps)
│   │   └── ru/spectech/domain/
│   │       ├── model/             ← User, Order, Bid, Equipment, etc.
│   │       ├── enums/             ← EquipmentCategory, PricingUnit, …
│   │       └── error/             ← ApiError sealed class
│   ├── network/                   ← Ktor client, HMAC, cert pinning
│   │   └── ru/spectech/network/
│   │       ├── ApiClient.kt
│   │       ├── HmacInterceptor.kt
│   │       ├── endpoints/         ← AuthApi, OrdersApi, EquipmentApi, …
│   │       └── ApiError.kt
│   ├── data/                      ← repositories + DTOs
│   │   └── ru/spectech/data/
│   │       ├── auth/AuthRepository.kt
│   │       ├── orders/OrdersRepository.kt
│   │       ├── equipment/EquipmentRepository.kt
│   │       └── …
│   ├── ui-kit/                    ← shared Compose components & theme
│   │   └── ru/spectech/uikit/
│   │       ├── theme/             ← Color.kt, Type.kt, Theme.kt
│   │       ├── components/        ← LoadingStateView, EmptyStateView, etc.
│   │       └── icons/             ← SF Symbol → Material Icon adapter
│   └── platform/                  ← secure storage, push, image, config
│       └── ru/spectech/platform/
│           ├── storage/SecureStorage.kt
│           ├── push/FcmService.kt
│           ├── image/ImageEncoder.kt
│           └── config/AppConfiguration.kt
├── features/                      ← one module per top-level feature
│   ├── auth/                      ← AuthSheet, OTP, Register
│   ├── marketplace/               ← MarketplaceList, FilterSheet, OrderDetail
│   ├── create-order/              ← CreateOrderScreen, view model
│   ├── orders/                    ← MyOrders, MyBids
│   ├── garage/                    ← GarageList, AddEquipment, EditEquipment, Deposit
│   ├── bidding/                   ← BidSheet
│   ├── profile/                   ← Profile, EditProfile
│   ├── news/                      ← NewsList
│   ├── notifications/             ← NotificationsList
│   └── support/                   ← SupportChat
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/libs.versions.toml      ← version catalog
```

## Dependency direction

Strictly **downward only**:

```
       ┌─── app ───┐
       │           │
   features      core/data
       │           │
       └─► core/ui-kit ─► core/network ─► core/domain
                        └─► core/platform ─►─┘
```

- `core/domain` depends on **nothing** (no Android, no Ktor, no Compose).
- `core/network` depends on `core/domain` only.
- `core/data` depends on `core/network` and `core/domain`.
- `core/ui-kit` depends on Compose + `core/domain` (for type-aware components
  like a category chip).
- `core/platform` is the Android-specific layer (Context, Keychain replacement,
  FCM, image encoder). It depends on `core/domain` for shared types only.
- `features/*` depend on `core/data`, `core/ui-kit`, `core/platform`. They do
  **not** depend on each other; cross-feature navigation goes through `app`.
- `app` wires everything together and contains the `NavHost`.

This is the Android-idiomatic way to express what iOS does via folders:

| iOS folder | Android module |
|---|---|
| `Shared/Models/DomainModels.swift` | `core/domain/model` |
| `Networking/API/` + `Networking/Endpoints/` | `core/network` |
| `Services/`, `Features/*/AuthService.swift` etc. | `core/data` (repositories) |
| `Design/`, `Shared/Views/` | `core/ui-kit` |
| `Infrastructure/Storage`, `Infrastructure/Support`, `App/AppDelegate` | `core/platform` |
| `Features/Marketplace`, `Features/Auth`, … | `features/marketplace`, `features/auth`, … |
| `App/SpecTechIOSApp.swift`, `Scene/Root/*` | `app/` |

## Package naming

- Root namespace: `ru.spectech.android` (replace with the company's actual
  domain if needed — iOS bundle id is `com.spectech.ios` per `KeychainStore.service`).
- Per-module: `ru.spectech.<modulename>` (e.g. `ru.spectech.network`).

## Single-module fallback

If a multi-module setup is overkill for the team size, collapse to:

```
app/src/main/kotlin/ru/spectech/android/
├── domain/
├── network/
├── data/
├── uikit/
├── platform/
└── features/
    ├── auth/
    ├── marketplace/
    └── …
```

…but keep the **package** boundaries and never let `domain` import from
anywhere else. The multi-module layout enforces this at the build level.
