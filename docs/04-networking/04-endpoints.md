# 04 — Endpoint Reference

Every backend endpoint the iOS app talks to, with method, path, auth
requirement, query/body shape, and response shape. The Android port must
hit these identically.

Base URL: `https://spectech-backoffice.onrender.com/api`

> All responses are wrapped in `{ "success": true, "data": <T> }`. The
> "Response" column shows the inner `<T>`.

## Auth

iOS: `SpecTechIOS/Networking/Endpoints/AuthAPI.swift`

| Endpoint | Auth | Body | Response |
|---|---|---|---|
| `POST /auth/send-otp` | No | `{ phone: string }` (E.164) | `{ phone: string, expiresIn: int? }` |
| `POST /auth/verify-otp` | No | `{ phone, code, name?, email?, city?, role?, accountType? }` | `{ token: string, user: VerifyOtpUserData, isNewUser: bool }` |

`VerifyOtpUserData` shape: `{ id, phone, role, name?, email?, city?, createdAt? }`.

## Orders

iOS: `SpecTechIOS/Networking/Endpoints/OrdersAPI.swift`

| Endpoint | Auth | Query / Body | Response |
|---|---|---|---|
| `GET /orders` | **No** (public for `view=marketplace`) | `view=marketplace|mine|pending|all`, `limit`, `offset`, `categories[]`, `regions[]`, `cities[]`, `pricingUnits[]`, `paymentTypes[]` | `Order[]` |
| `POST /orders` | Yes | `CreateOrderRequest` | `Order` |
| `DELETE /orders/{id}` | Yes | — | `{ id: string? }` |
| `POST /orders/{orderId}/bids` | Yes | `CreateBidRequest` | `Bid` |
| `POST /orders/{orderId}/bids/{bidId}/accept` | Yes | — | `ContractorContact` |
| `DELETE /orders/{orderId}/bids/{bidId}` | Yes | — | `{ id: string? }` (withdrawn) |
| `GET /users/{id}` | Yes | — | `{ id, phone, name, role }` (contractor lookup) |

Filter query param **encoding rules** (iOS does this):
- Categories: snake_case codes (`categories=dump_truck&categories=excavator_crawler`)
- Regions: raw strings
- Cities: raw strings
- PricingUnits: snake_case codes
- PaymentTypes: snake_case codes
- Each appears as a repeated query param (no comma-separated lists).
- Sort alphabetically before sending so identical filter sets produce identical
  URLs (helps with caching and HMAC sanity checks).

## Equipment

iOS: `SpecTechIOS/Networking/Endpoints/EquipmentAPI.swift`

| Endpoint | Auth | Body | Response |
|---|---|---|---|
| `GET /equipment` | Yes | — | `Equipment[]` (with optional `depositStatus`, `depositId`) |
| `POST /equipment` | Yes | `CreateEquipmentRequest` | `Equipment` |
| `PATCH /equipment/{id}` | Yes | `UpdateEquipmentRequest` (partial) | `Equipment` |
| `DELETE /equipment/{id}` | Yes | — | `{ id: string? }` |
| `POST /equipment/photos/upload` | Yes | `multipart/form-data; file=<jpeg>` | `{ url: string }` |

Multipart field name **must be** `file`, filename `photo.jpg`, MIME
`image/jpeg`. See iOS `EquipmentService.buildMultipartBody` and ImageEncoder.

## Profile

iOS: `SpecTechIOS/Networking/Endpoints/ProfileAPI.swift`

| Endpoint | Auth | Body | Response |
|---|---|---|---|
| `PATCH /users/me/profile` | Yes | `{ name, email, city }` | `{ id, phone, name?, email?, city?, role }` |

## Push (notifications/register)

iOS: `SpecTechIOS/Networking/Endpoints/PushAPI.swift`

| Endpoint | Auth | Body | Response |
|---|---|---|---|
| `POST /notifications/register` | Yes | `{ token: string, platform: "ios" }` → use `"android"` on Android | `{ registered: bool? }` |
| `POST /notifications/unregister` | Yes | `{ token: string }` | `{ unregistered: bool? }` |

## Saved filter (server-side new-order notifications)

iOS: `SpecTechIOS/Networking/Endpoints/SavedFilterAPI.swift`

| Endpoint | Auth | Body | Response |
|---|---|---|---|
| `GET /notifications/saved-filter` | Yes | — | `{ filters?: SavedFilterPayload, enabled?: bool, updatedAt?: string }` (404 if none) |
| `PUT /notifications/saved-filter` | Yes | `{ filters: SavedFilterPayload, enabled: bool }` | (same as GET response) |
| `POST /notifications/saved-filter/enabled` | Yes | `{ enabled: bool }` | (same shape) |
| `DELETE /notifications/saved-filter` | Yes | — | `{ deleted: bool? }` |

`SavedFilterPayload`:

```kotlin
@Serializable
data class SavedFilterPayload(
    val categories: List<String>,    // snake_case codes
    val regions: List<String>,
    val cities: List<String>,
    val pricingUnits: List<String>,
    val paymentTypes: List<String>,
)
```

## News

iOS: `SpecTechIOS/Networking/Endpoints/NewsAPI.swift`

| Endpoint | Auth | Response |
|---|---|---|
| `GET /news` | **No** | `NewsItem[]` |

## Support

iOS: `SpecTechIOS/Networking/Endpoints/SupportAPI.swift`

| Endpoint | Auth | Body | Response |
|---|---|---|---|
| `POST /support/messages` | **No** | `{ message, contactInfo?, relatedEntityType?, relatedEntityId? }` | `{ id?, status? }` |

`contactInfo` is the contractor's phone (the iOS view model pulls it from
`SessionStore.currentUser?.phone`).

## Deposit (YooKassa)

iOS: `SpecTechIOS/Networking/Endpoints/DepositAPI.swift`

| Endpoint | Auth | Body | Response |
|---|---|---|---|
| `POST /payments/deposits` | Yes | `{ equipmentId, returnUrl?, savePaymentMethod }` | `Deposit` |
| `GET /payments/deposits/equipment/{id}` | Yes | — | `DepositForEquipmentPayload` |
| `POST /payments/deposits/{id}/sync` | Yes | — | `Deposit` (refreshed status) |
| `POST /payments/deposits/{id}/refund` | Yes | `{ reason? }` | `Deposit` |
| `GET /payments/deposits` | Yes | — | `Deposit[]` |

`returnUrl` — when set, YooKassa redirects there after payment. On Android,
use a deep link such as `spectech://deposit/{equipmentId}` and register it
in the manifest. On the dismiss of Chrome Custom Tabs, call
`POST /payments/deposits/{id}/sync` to refresh status.

## Kotlin endpoint catalog

```kotlin
sealed interface ApiTarget { /* see 01-api-client.md */ }

object AuthApi {
    @Serializable data class SendOtpBody(val phone: String)
    @Serializable data class VerifyOtpBody(
        val phone: String, val code: String,
        val name: String?, val email: String?, val city: String?,
        val role: String?, val accountType: String?,
    )

    data class SendOtp(val body: SendOtpBody) : ApiTarget {
        override val path = "auth/send-otp"
        override val method = HttpMethod.Post
        override val requiresAuth = false
        override val body get() = Json.encodeToString(body).toByteArray()
    }
    data class VerifyOtp(val body: VerifyOtpBody) : ApiTarget { … }
}

object OrdersApi {
    data class FetchOrders(val scope: OrderScope, val limit: Int, val offset: Int, val filters: OrderFilters?) : ApiTarget {
        override val path = "orders"
        override val method = HttpMethod.Get
        override val requiresAuth = false
        override val queryItems get() = buildList {
            add("view" to scope.wire)
            add("limit" to limit.toString())
            add("offset" to offset.toString())
            filters?.takeIf { !it.isEmpty }?.let {
                it.categories.sortedBy { c -> c.wire }.forEach { c -> add("categories" to c.wire) }
                it.regions.sorted().forEach { r -> add("regions" to r) }
                it.selectedCities.sorted().forEach { c -> add("cities" to c) }
                it.pricingUnits.sortedBy { u -> u.wire }.forEach { u -> add("pricingUnits" to u.wire) }
                it.paymentTypes.sortedBy { p -> p.wire }.forEach { p -> add("paymentTypes" to p.wire) }
            }
        }
    }

    data class CreateOrder(val req: CreateOrderRequest) : ApiTarget { … }
    data class DeleteOrder(val id: Uuid) : ApiTarget {
        override val path = "orders/${id.toString().lowercase()}"
        override val method = HttpMethod.Delete
    }
    data class SubmitBid(val orderId: Uuid, val req: CreateBidRequest) : ApiTarget { … }
    data class AcceptBid(val orderId: Uuid, val bidId: Uuid) : ApiTarget {
        override val path = "orders/${orderId.toString().lowercase()}/bids/${bidId.toString().lowercase()}/accept"
        override val method = HttpMethod.Post
    }
    data class WithdrawBid(val orderId: Uuid, val bidId: Uuid) : ApiTarget {
        override val path = "orders/${orderId.toString().lowercase()}/bids/${bidId.toString().lowercase()}"
        override val method = HttpMethod.Delete
    }
    data class FetchContractor(val id: Uuid) : ApiTarget {
        override val path = "users/${id.toString().lowercase()}"
        override val method = HttpMethod.Get
    }
}
```

Repeat for `EquipmentApi`, `ProfileApi`, `PushApi`, `SavedFilterApi`,
`NewsApi`, `SupportApi`, `DepositApi`. Each one is a direct
1:1 port of its iOS counterpart.

## A single shared cheat sheet

See [99-appendix/01-endpoint-cheatsheet.md](../99-appendix/01-endpoint-cheatsheet.md).
