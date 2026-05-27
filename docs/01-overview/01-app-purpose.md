# 01 — App Purpose

## What SpecTech is

**SpecTech** is a Russian B2B marketplace for renting heavy construction
equipment — excavators, cranes, dump trucks, concrete pumps, graders, road
rollers, etc. (27 categories in total, see [domain/02-enums.md](../03-domain/02-enums.md)).

Two user roles share **one** app:

- **Customer** — creates a rental order specifying the equipment type, dates,
  location, payment terms.
- **Contractor** — browses the marketplace, submits bids with their own
  equipment and price; the customer accepts the best bid.

After bid acceptance, the contact info (phone) is revealed between the two
parties and they coordinate offline. The platform itself does not handle
payments for the rental — only for a **performance security deposit** the
contractor posts before being allowed to bid (YooKassa-hosted).

## Order lifecycle

```
            ┌───────────┐  customer        ┌──────────┐
created ──► │   open    │  accepts a bid ► │ accepted │
            └─────┬─────┘                  └────┬─────┘
                  │                             │
       expired/cancelled                   in_progress
                                                │
                                              completed
                                                │
                                              closed
```

Status enum values (must match the backend exactly): `open`, `pending`,
`accepted`, `in_progress`, `completed`, `cancelled`, `expired`, `closed`.

iOS source: `SpecTechIOS/Shared/Models/DomainModels.swift` enum `OrderStatus`.

## Tabs (bottom navigation)

The authenticated app has **five tabs**:

1. **Announcements** (Marketplace) — public list of open orders, filterable
2. **My Bids** — orders the contractor has bid on (pending)
3. **My Orders** — orders the customer created (mine)
4. **Garage** — contractor's equipment inventory
5. **News** — platform news feed

Top-bar actions on every tab: **Support**, **Notifications**, **Profile**.
The Marketplace tab additionally has a **Create Order** action.

Unauthenticated users still see Marketplace and News tabs (read-only), with a
**Sign In** call-to-action replacing actions that require auth.

## Surfaces that are NOT tabs

- **Auth sheet** — full-screen sheet on top of any tab when the user signs in
  or registers. Contains its own internal `NavigationStack`
  (start → register → verify OTP).
- **Profile sheet** — modal sheet from the top-bar profile button.
- **Create Order sheet** — modal sheet, also used in "Republish" flow.
- **Bid sheet** — modal from order detail.
- **Support chat sheet**, **Notifications sheet** — modals from the top bar.

## Localization

Two languages: **English** and **Russian**. The user toggles via Profile.

iOS uses `String(localized:)` + `Localizable.xcstrings` (4670 lines).
Android target: `res/values/strings.xml` + `res/values-ru/strings.xml`.

Some Russian strings are sent to the backend as canonical values
(e.g. equipment category `"Самосвал"`, payment type `"наличные"`). These are
**not** for display; they go in the request body and are the same regardless
of UI language. See [domain/02-enums.md](../03-domain/02-enums.md).

## Non-goals (what NOT to build)

- No in-app rental payment processing. Only the deposit is paid via YooKassa.
- No chat between customer and contractor. They get each other's phone after
  bid acceptance and talk offline.
- No admin tooling. Admin role exists in the enum but the iOS app does not
  ship any admin UI.
