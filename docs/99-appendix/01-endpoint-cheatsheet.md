# 01 — Endpoint Cheatsheet

Single-page reference for every backend endpoint. Base URL:
`https://spectech-backoffice.onrender.com/api`

Every request also carries: `X-Client-Id`, `X-Timestamp`, `X-Nonce`,
`X-Signature` (see [04-networking/02-hmac-signing.md](../04-networking/02-hmac-signing.md))
plus `Authorization: Bearer <jwt>` when `Auth = Yes`.

All success responses wrap data in `{ "success": true, "data": <T> }`.

## Auth

| # | Method | Path | Auth | Body | Returns |
|---|---|---|---|---|---|
| 1 | POST | `/auth/send-otp` | No | `{ phone }` | `{ phone, expiresIn? }` |
| 2 | POST | `/auth/verify-otp` | No | `{ phone, code, name?, email?, city?, role?, accountType? }` | `{ token, user: VerifyOtpUser, isNewUser }` |

## Orders

| # | Method | Path | Auth | Body / Query | Returns |
|---|---|---|---|---|---|
| 3 | GET | `/orders` | (No for marketplace) | `?view=…&limit&offset&categories[]&regions[]&cities[]&pricingUnits[]&paymentTypes[]` | `Order[]` |
| 4 | POST | `/orders` | Yes | `CreateOrderRequest` | `Order` |
| 5 | DELETE | `/orders/{id}` | Yes | — | `{ id? }` |
| 6 | POST | `/orders/{id}/bids` | Yes | `CreateBidRequest` | `Bid` |
| 7 | POST | `/orders/{id}/bids/{bidId}/accept` | Yes | — | `{ phone?, name? }` |
| 8 | DELETE | `/orders/{id}/bids/{bidId}` | Yes | — | `{ id? }` |
| 9 | GET | `/users/{id}` | Yes | — | `{ id, phone, name, role }` |

## Equipment

| # | Method | Path | Auth | Body | Returns |
|---|---|---|---|---|---|
| 10 | GET | `/equipment` | Yes | — | `Equipment[]` |
| 11 | POST | `/equipment` | Yes | `CreateEquipmentRequest` | `Equipment` |
| 12 | PATCH | `/equipment/{id}` | Yes | `UpdateEquipmentRequest` (partial) | `Equipment` |
| 13 | DELETE | `/equipment/{id}` | Yes | — | `{ id? }` |
| 14 | POST | `/equipment/photos/upload` | Yes | `multipart/form-data file=…` | `{ url }` |

## Profile

| # | Method | Path | Auth | Body | Returns |
|---|---|---|---|---|---|
| 15 | PATCH | `/users/me/profile` | Yes | `{ name, email, city }` | `{ id, phone, name?, email?, city?, role }` |

## Push

| # | Method | Path | Auth | Body | Returns |
|---|---|---|---|---|---|
| 16 | POST | `/notifications/register` | Yes | `{ token, platform: "android" }` | `{ registered? }` |
| 17 | POST | `/notifications/unregister` | Yes | `{ token }` | `{ unregistered? }` |

## Saved filter

| # | Method | Path | Auth | Body | Returns |
|---|---|---|---|---|---|
| 18 | GET | `/notifications/saved-filter` | Yes | — | `{ filters?, enabled?, updatedAt? }` |
| 19 | PUT | `/notifications/saved-filter` | Yes | `{ filters, enabled }` | same |
| 20 | POST | `/notifications/saved-filter/enabled` | Yes | `{ enabled }` | same |
| 21 | DELETE | `/notifications/saved-filter` | Yes | — | `{ deleted? }` |

## News

| # | Method | Path | Auth | Returns |
|---|---|---|---|---|
| 22 | GET | `/news` | **No** | `NewsItem[]` |

## Support

| # | Method | Path | Auth | Body | Returns |
|---|---|---|---|---|---|
| 23 | POST | `/support/messages` | **No** | `{ message, contactInfo?, relatedEntityType?, relatedEntityId? }` | `{ id?, status? }` |

## Deposit (YooKassa)

| # | Method | Path | Auth | Body | Returns |
|---|---|---|---|---|---|
| 24 | POST | `/payments/deposits` | Yes | `{ equipmentId, returnUrl?, savePaymentMethod }` | `Deposit` |
| 25 | GET | `/payments/deposits/equipment/{id}` | Yes | — | `{ equipmentId, deposit?, isPaid }` |
| 26 | POST | `/payments/deposits/{id}/sync` | Yes | — | `Deposit` |
| 27 | POST | `/payments/deposits/{id}/refund` | Yes | `{ reason? }` | `Deposit` |
| 28 | GET | `/payments/deposits` | Yes | — | `Deposit[]` |

## Error envelope

Failure responses (any non-2xx) look like:

```json
{
  "error": {
    "code": "DEPOSIT_REQUIRED",
    "message": "Deposit not paid.",
    "details": [
      { "field": "equipmentId", "message": "no active deposit" }
    ]
  }
}
```

…or a top-level `{ "message": "…" }`. The `ApiClient` decodes both.

## Special status codes the app reacts to

| Status | Meaning | App reaction |
|---|---|---|
| 401 | Unauthorized | Clear session, surface "Sign in again" |
| 402 / `DEPOSIT_REQUIRED` | Contractor's equipment has no paid deposit | Show "Pay deposit" CTA in bid sheet |
| 404 (saved-filter) | No saved filter on server | No-op, keep local state |
| 429 | Rate limited | Show "Too many attempts" |
| 502 / 503 / 504 | Render cold start / transient backend | Retry up to 2× with 2s × attempt delay |
