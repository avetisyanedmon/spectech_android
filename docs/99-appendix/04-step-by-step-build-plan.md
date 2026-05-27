# 04 — Step-by-Step Build Plan

A recommended order of work for porting the iOS app. Each phase is shippable
to a real device in some form — you can stop at the end of any phase and
have a working Android app at that level of completeness.

## Phase 0 — Scaffolding (1 day)

- [ ] Create Android Studio project at `spectech_android/`
- [ ] Set up Gradle multi-module layout per [02-architecture/01-module-layout.md](../02-architecture/01-module-layout.md)
- [ ] Configure version catalog per [08-testing-build/02-gradle-setup.md](../08-testing-build/02-gradle-setup.md)
- [ ] Configure `AppConfiguration` + `BuildConfig` per [07-infrastructure/04-app-configuration.md](../07-infrastructure/04-app-configuration.md)
- [ ] Add Hilt to root + every module
- [ ] App compiles, MainActivity shows "Hello SpecTech"

## Phase 1 — Domain & Network (2 days)

- [ ] Port all enums per [03-domain/02-enums.md](../03-domain/02-enums.md) — write unit tests for the lenient decoders
- [ ] Port `User`, `AuthSession`, `Order`, `Bid`, `Equipment`, `Deposit`, `NewsItem`, `AppNotification`, `OrderFilters` (each gets one test)
- [ ] Build `ApiClient` with Ktor + OkHttp per [04-networking/01-api-client.md](../04-networking/01-api-client.md)
- [ ] Implement `HmacInterceptor` per [04-networking/02-hmac-signing.md](../04-networking/02-hmac-signing.md) — write the golden-output test
- [ ] Implement `CertificatePinner` per [04-networking/03-cert-pinning.md](../04-networking/03-cert-pinning.md) (skip pinning in DEBUG initially)
- [ ] Port all endpoint targets (`AuthApi`, `OrdersApi`, `EquipmentApi`, `ProfileApi`, `PushApi`, `SavedFilterApi`, `NewsApi`, `SupportApi`, `DepositApi`)
- [ ] Write `ApiError` envelope decoder

✅ At this point you can write integration tests that hit the real backend
from JVM. Verify with `curl` + a manually-signed request first.

## Phase 2 — Auth + Session restore (2 days)

- [ ] `SecureStorage` + `EncryptedSharedPreferences` wrapper per [07-infrastructure/01-secure-storage.md](../07-infrastructure/01-secure-storage.md)
- [ ] `SessionStore` per [03-domain/03-user-and-session.md](../03-domain/03-user-and-session.md)
- [ ] `AuthRepository` + `AuthFlowViewModel`
- [ ] `RussianPhoneFormatter` + `PhoneNormalizer` per [05-features/01-auth.md](../05-features/01-auth.md)
- [ ] Splash screen with `installSplashScreen` waiting on session restore
- [ ] Auth sheet (`StartAuthScreen` + `RegisterScreen` + `VerifyOtpScreen`)
- [ ] Logout button somewhere (e.g. a temporary screen)

✅ You can sign in, see the JWT in `SessionStore`, and sign out.

## Phase 3 — Theming & shared UI (1 day)

- [ ] `SpecTechTheme` with brand-blue colors per [06-ui/01-design-tokens.md](../06-ui/01-design-tokens.md)
- [ ] `LoadingStateView`, `EmptyStateView`, `ErrorStateView`, `SignInPromptView`
- [ ] `OrderStatusBadge`, `EquipmentHeroImage`, `PhoneActionButton`
- [ ] Coil image loader configured per [06-ui/05-images-and-icons.md](../06-ui/05-images-and-icons.md)
- [ ] App icon + launch screen
- [ ] Per-app locale wiring (en + ru) per [06-ui/04-localization.md](../06-ui/04-localization.md)

## Phase 4 — Main navigation (1 day)

- [ ] `MainTabsScreen` with `NavigationBar` + 5 tabs per [02-architecture/03-navigation.md](../02-architecture/03-navigation.md)
- [ ] Nested `NavHost` per tab (save/restore state)
- [ ] Top-bar actions: Support, Notifications, Profile (placeholder sheets for now)

## Phase 5 — Marketplace + Order Detail (2 days)

- [ ] `MarketplaceViewModel` + screen per [05-features/02-marketplace.md](../05-features/02-marketplace.md)
- [ ] `MarketplaceFilterSheet`
- [ ] `OrderDetailScreen`
- [ ] Pagination wired
- [ ] `AppEventBus` + `OrdersChanged` reload

## Phase 6 — Create Order (3 days)

- [ ] `CreateOrderViewModel` with all 50+ fields per [05-features/03-create-order.md](../05-features/03-create-order.md)
- [ ] Category-specific subforms (`when (category) → render this section`)
- [ ] `buildOptionsSummary()` port
- [ ] Date/time pickers
- [ ] `CitySearchField` with Google Places (requires Places API key)
- [ ] `RegionPickerField`
- [ ] Submit + success state
- [ ] Republish prefill

## Phase 7 — My Orders + My Bids (1 day)

- [ ] `MyOrdersViewModel` + screen per [05-features/04-my-orders.md](../05-features/04-my-orders.md)
- [ ] `MyBidsViewModel` + screen per [05-features/05-my-bids.md](../05-features/05-my-bids.md)
- [ ] Accept-bid flow + contractor contact reveal
- [ ] Withdraw-bid flow

## Phase 8 — Garage + Add/Edit Equipment (2 days)

- [ ] `GarageViewModel` + screen per [05-features/07-garage.md](../05-features/07-garage.md)
- [ ] `AddEquipmentViewModel` + screen
- [ ] `EditEquipmentViewModel` + screen
- [ ] Photo picker + concurrent uploads per [07-infrastructure/03-image-encoding-upload.md](../07-infrastructure/03-image-encoding-upload.md)
- [ ] `EquipmentDetailScreen`

## Phase 9 — Bidding (1 day)

- [ ] `BidSheetViewModel` + screen per [05-features/06-bidding.md](../05-features/06-bidding.md)
- [ ] Inline "Add Equipment" entry from the bid sheet
- [ ] `DEPOSIT_REQUIRED` error mapping

## Phase 10 — Deposit + YooKassa (2 days)

- [ ] `DepositRepository` + `DepositService` calls per [05-features/08-deposit.md](../05-features/08-deposit.md)
- [ ] `DepositInfoSheet`
- [ ] Chrome Custom Tabs for YooKassa
- [ ] `sync` on tab close
- [ ] Refund flow

## Phase 11 — Notifications & Push (2 days)

- [ ] Firebase project + `google-services.json`
- [ ] `FcmService` per [07-infrastructure/02-push-notifications.md](../07-infrastructure/02-push-notifications.md)
- [ ] `PushRepository` + token registration
- [ ] `NotificationStore` (in-app inbox)
- [ ] `NotificationsSheet`
- [ ] Deep-link routing per [05-features/11-notifications.md](../05-features/11-notifications.md)
- [ ] `POST_NOTIFICATIONS` runtime permission flow
- [ ] **Coordinate with backend: switch FCM payload schema for Android per the doc**

## Phase 12 — Profile + Localization toggle (1 day)

- [ ] `ProfileScreen` per [05-features/09-profile.md](../05-features/09-profile.md)
- [ ] `EditProfileScreen`
- [ ] `ProfileStore` + `LocalProfile`
- [ ] Language switcher via `AppCompatDelegate.setApplicationLocales`
- [ ] Privacy / Terms URL opening
- [ ] Logout (already wired in Phase 2 — now move into Profile)

## Phase 13 — Saved Filters (1 day)

- [ ] `SavedFilterStore` per [05-features/13-saved-filters.md](../05-features/13-saved-filters.md)
- [ ] Hook into `MarketplaceFilterSheet`
- [ ] Surface in Profile

## Phase 14 — News + Support (1 day)

- [ ] `NewsStore` + screen per [05-features/10-news.md](../05-features/10-news.md)
- [ ] `SupportChatSheet` per [05-features/12-support-chat.md](../05-features/12-support-chat.md)

## Phase 15 — Localization + polish (2 days)

- [ ] Run the `.xcstrings` → `strings.xml` migration script per [06-ui/04-localization.md](../06-ui/04-localization.md)
- [ ] Audit for hardcoded strings; extract every `Text("…")` to resources
- [ ] Lint passes with no `MissingTranslation` warnings
- [ ] Russian + English UI walkthrough

## Phase 16 — Testing + CI (2 days)

- [ ] Unit tests for HMAC, decoders, view models per [08-testing-build/01-testing-strategy.md](../08-testing-build/01-testing-strategy.md)
- [ ] GitHub Actions per [08-testing-build/03-ci-cd.md](../08-testing-build/03-ci-cd.md)
- [ ] Detekt + ktlint configured
- [ ] Coverage report

## Phase 17 — Release prep (2 days)

- [ ] Signing config per [08-testing-build/04-release.md](../08-testing-build/04-release.md)
- [ ] Enable cert pinning for release builds
- [ ] Internal Play Console track
- [ ] Privacy policy / data safety in Play Console
- [ ] Beta with real users

## Total: ~25 working days for full parity

Cut to ~15 days if you defer:
- YooKassa deposit flow (Phase 10) — only matters for contractors who bid
- Saved filters / push (Phases 11, 13) — read-only Android works without push
- News + Support (Phase 14) — non-essential
- The 27 category-specific subforms in Create Order — start with the
  required fields, layer the optional ones in over time

…and ship a v0.5 Android with marketplace browse + bid + my orders +
profile. Backfill the rest in subsequent releases.

## What goes into v1.0 vs later

v1.0 must-haves: phases 0–9, 11 (push), 12, 14 (support at minimum), 15 (Russian)
v1.1 nice-to-haves: phase 10 (deposit), 13 (saved-filter notifications)
