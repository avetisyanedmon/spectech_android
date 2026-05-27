# 01 — Domain Overview

The domain module is **pure Kotlin** (no Android, no Ktor, no Compose) and
holds all business types: models, enums, request/response shapes, value
objects. It's the Android counterpart to `SpecTechIOS/Shared/Models/DomainModels.swift`.

## Why "pure"

- Reusable across modules (`network`, `data`, `ui-kit`, `features/*`).
- Trivially unit-testable without an Android emulator.
- Survives rename of UI/networking stacks (Compose → anything later, etc.).

## Conventions

| Concern | Choice |
|---|---|
| Numeric IDs (UUID) | `kotlin.uuid.Uuid` (Kotlin 2.0+) or `java.util.UUID`. Pick one and stick to it. |
| Money | `java.math.BigDecimal`. iOS uses `Decimal` — same semantics. |
| Dates | `kotlinx.datetime.Instant` for wire-format dates; `LocalDate` / `LocalDateTime` only when explicitly local-bound. |
| Optional fields | Kotlin `?`-nullable types. Match iOS `Optional<T>` exactly. |
| Equality | All models are `data class` (auto-generated `equals`/`hashCode`/`copy`). |
| Immutability | All fields `val`. iOS uses `let` — same constraint. |
| Serialization | `@Serializable` from kotlinx-serialization. |
| Defaulting | Use Kotlin default arguments only where the iOS init has defaults. Otherwise require all fields. |

## File map

| iOS type | Android file | Notes |
|---|---|---|
| `UserRole` | `enums/UserRole.kt` | enum w/ `customer`, `contractor`, `admin` |
| `EquipmentCategory` | `enums/EquipmentCategory.kt` | 27 cases + Russian backend value + lenient decoder |
| `PricingUnit` | `enums/PricingUnit.kt` | 9 cases + Russian backend value |
| `PaymentType` | `enums/PaymentType.kt` | 3 cases |
| `OrderScope` | `enums/OrderScope.kt` | query value |
| `OrderStatus` | `enums/OrderStatus.kt` | lenient decoder for `"in progress"` / `"canceled"` variants |
| `DepositStatus` | `enums/DepositStatus.kt` | + `blocksNewDeposit` helper |
| `AppLanguage` | `enums/AppLanguage.kt` | `english`, `russian` |
| `User` | `model/User.kt` | |
| `AuthSession` | `model/AuthSession.kt` | |
| `ContractorInfo` | `model/ContractorInfo.kt` | |
| `Bid` | `model/Bid.kt` | embedded contractor info handling |
| `Order` | `model/Order.kt` | computed `displayAddress`, `fullAddress`, `isExpired` |
| `Equipment` | `model/Equipment.kt` | `withDeposit(...)` copy helper |
| `Deposit` | `model/Deposit.kt` | |
| `OrderFilters` | `model/OrderFilters.kt` | `matches(order)` + `isEmpty` |
| `CreateOrderRequest` | `request/CreateOrderRequest.kt` | |
| `CreateBidRequest` | `request/CreateBidRequest.kt` | |
| `CreateEquipmentRequest` | `request/CreateEquipmentRequest.kt` | |
| `UpdateEquipmentRequest` | `request/UpdateEquipmentRequest.kt` | all-optional |
| `RegistrationProfile` | `model/RegistrationProfile.kt` | with `AccountType` nested enum |
| `LocalProfile` | `model/LocalProfile.kt` | settings stored in EncryptedSharedPreferences |
| `NewsItem` | `model/NewsItem.kt` | |
| `AppNotification` | `model/AppNotification.kt` | with `dedupeKey` |
| `ContractorContact` | `model/ContractorContact.kt` | |
| `RemoteState<T>` | `state/RemoteState.kt` | sealed interface (see [04-state-management.md](../02-architecture/04-state-management.md)) |
| `ApiError` | `error/ApiError.kt` | sealed data class (see [04-networking/01-api-client.md](../04-networking/01-api-client.md)) |

Subsequent files in this folder describe the trickier types in detail
(enums with Russian backend mappings, the Bid contractor-resolution logic,
order computed properties, and filter matching).
