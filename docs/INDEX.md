# Documentation Index

## 01 — Overview
- [01-app-purpose.md](01-overview/01-app-purpose.md) — what SpecTech does, user roles, business rules
- [02-tech-stack.md](01-overview/02-tech-stack.md) — chosen Android libraries with justification
- [03-feature-parity-checklist.md](01-overview/03-feature-parity-checklist.md) — every iOS feature and its Android target

## 02 — Architecture
- [01-module-layout.md](02-architecture/01-module-layout.md) — Gradle modules, package structure
- [02-dependency-injection.md](02-architecture/02-dependency-injection.md) — Hilt graph, mapping iOS `@Environment` → Hilt
- [03-navigation.md](02-architecture/03-navigation.md) — `NavigationStack` → Navigation Compose; per-tab back stacks
- [04-state-management.md](02-architecture/04-state-management.md) — `@Observable` → `ViewModel` + `StateFlow`
- [05-concurrency.md](02-architecture/05-concurrency.md) — `async/await` + `@MainActor` → coroutines + `Dispatchers.Main`
- [06-app-startup.md](02-architecture/06-app-startup.md) — `SpecTechIOSApp` → `Application` + `MainActivity`

## 03 — Domain
- [01-overview.md](03-domain/01-overview.md) — pure-Kotlin domain module
- [02-enums.md](03-domain/02-enums.md) — `EquipmentCategory`, `PricingUnit`, `PaymentType`, `OrderScope`, `OrderStatus`, `DepositStatus`, `UserRole`, `AppLanguage`
- [03-user-and-session.md](03-domain/03-user-and-session.md) — `User`, `AuthSession`, `LocalProfile`, `ContractorInfo`
- [04-order-and-bid.md](03-domain/04-order-and-bid.md) — `Order`, `Bid`, `OrderFilters`, `CreateOrderRequest`, `CreateBidRequest`
- [05-equipment-and-deposit.md](03-domain/05-equipment-and-deposit.md) — `Equipment`, `Deposit`, request shapes
- [06-news-and-notification.md](03-domain/06-news-and-notification.md) — `NewsItem`, `AppNotification`

## 04 — Networking
- [01-api-client.md](04-networking/01-api-client.md) — Ktor client, retries, error envelope, snake_case decoding
- [02-hmac-signing.md](04-networking/02-hmac-signing.md) — exact byte-by-byte signing algorithm (must match iOS)
- [03-cert-pinning.md](04-networking/03-cert-pinning.md) — OkHttp `CertificatePinner` with the same SHA-256 hashes
- [04-endpoints.md](04-networking/04-endpoints.md) — every endpoint, method, path, query, body, response

## 05 — Features
- [01-auth.md](05-features/01-auth.md) — phone OTP, registration, session restore
- [02-marketplace.md](05-features/02-marketplace.md) — public list, filters, pagination
- [03-create-order.md](05-features/03-create-order.md) — 50+ field multi-step form
- [04-my-orders.md](05-features/04-my-orders.md) — customer's own orders, accept bids, republish
- [05-my-bids.md](05-features/05-my-bids.md) — contractor's pending bids, withdraw
- [06-bidding.md](05-features/06-bidding.md) — submit a bid sheet
- [07-garage.md](05-features/07-garage.md) — equipment CRUD, photo upload
- [08-deposit.md](05-features/08-deposit.md) — performance security deposit, YooKassa flow
- [09-profile.md](05-features/09-profile.md) — profile, edit, language toggle
- [10-news.md](05-features/10-news.md) — news feed
- [11-notifications.md](05-features/11-notifications.md) — in-app inbox, deep linking
- [12-support-chat.md](05-features/12-support-chat.md) — support message form
- [13-saved-filters.md](05-features/13-saved-filters.md) — saved filter + new-order notifications

## 06 — UI
- [01-design-tokens.md](06-ui/01-design-tokens.md) — colors, typography, spacing, brand blue
- [02-theme-and-material3.md](06-ui/02-theme-and-material3.md) — light/dark, Material 3 theming
- [03-shared-components.md](06-ui/03-shared-components.md) — loading/empty/error views, OTP digit box, phone field, etc.
- [04-localization.md](06-ui/04-localization.md) — `.xcstrings` → `strings.xml` migration plan (4670+ lines)
- [05-images-and-icons.md](06-ui/05-images-and-icons.md) — Coil setup, SF Symbol → Material Icon map, launcher icon
- [06-status-and-category-mapping.md](06-ui/06-status-and-category-mapping.md) — colors and labels per enum

## 07 — Infrastructure
- [01-secure-storage.md](07-infrastructure/01-secure-storage.md) — Keychain → EncryptedSharedPreferences
- [02-push-notifications.md](07-infrastructure/02-push-notifications.md) — APNs → FCM, token lifecycle, payload schema
- [03-image-encoding-upload.md](07-infrastructure/03-image-encoding-upload.md) — JPEG resize, multipart upload
- [04-app-configuration.md](07-infrastructure/04-app-configuration.md) — `AppConfiguration` → `BuildConfig`
- [05-logging.md](07-infrastructure/05-logging.md) — `APILogger` → Timber, DEBUG-only

## 08 — Testing, Build, Release
- [01-testing-strategy.md](08-testing-build/01-testing-strategy.md) — unit / instrumentation / UI tests
- [02-gradle-setup.md](08-testing-build/02-gradle-setup.md) — module wiring, version catalog, ProGuard
- [03-ci-cd.md](08-testing-build/03-ci-cd.md) — GitHub Actions workflow recommendation
- [04-release.md](08-testing-build/04-release.md) — signing config, Play Console rollout

## 99 — Appendix
- [01-endpoint-cheatsheet.md](99-appendix/01-endpoint-cheatsheet.md) — single-page API reference
- [02-ios-android-symbol-map.md](99-appendix/02-ios-android-symbol-map.md) — every iOS type → Android counterpart
- [03-glossary.md](99-appendix/03-glossary.md) — domain terms used in code (RU/EN)
- [04-step-by-step-build-plan.md](99-appendix/04-step-by-step-build-plan.md) — recommended implementation order
