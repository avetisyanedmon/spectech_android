# 03 — Feature Parity Checklist

This list enumerates every user-visible behavior in the iOS app. The Android
build is done when each row's "Android" cell is also ✅.

## Authentication & session

| iOS behavior | Source | Android |
|---|---|---|
| Phone OTP send (`POST auth/send-otp`) | `AuthService.startAuth` | ☐ |
| Phone normalization to E.164 `+7XXXXXXXXXX` | `PhoneNormalizer` | ☐ |
| OTP verify with optional registration profile | `AuthService.verifyAuth` | ☐ |
| 60-second resend countdown | `VerifyOTPView.startResendTimer` | ☐ |
| Auto-paste 6-digit code (system OTP autofill) | `OTPDigitBox.textContentType(.oneTimeCode)` | ☐ (use `autofillHints = listOf(AUTOFILL_HINT_SMS_OTP_CODE)`) |
| Persist `AuthSession` in Keychain | `SessionStore.save/restore` | ☐ EncryptedSharedPreferences |
| Restore session on app launch | `SpecTechIOSApp.task` | ☐ in Application.onCreate |
| Clear session on 401 from any endpoint | `APIClient.executeRequest` 401 path | ☐ |
| Dev bypass flag `bypassAuthFlow` (debug builds only) | `AppConfiguration` | ☐ |

## Marketplace

| iOS behavior | Source | Android |
|---|---|---|
| Public read of `GET orders?view=marketplace` (no auth required) | `OrdersAPI.fetchOrders` | ☐ |
| Hide expired orders (client-side `isExpired`) | `MarketplaceListViewModel.visibleOrders` | ☐ |
| Filters: categories, regions, cities, pricingUnits, paymentTypes | `OrderFilters` + `MarketplaceFilterSheet` | ☐ |
| Filter persistence per session (in memory) + saved-filter sync | `SavedFilterStore` | ☐ |
| Pagination: 50 per page, infinite scroll | `loadMoreIfNeeded` | ☐ |
| Auto-reload on `ordersDidChange` notification | `NotificationCenter` observer | ☐ EventBus |
| Tap → order detail | `NavigationStack` + `navigationDestination(for: Order.self)` | ☐ |
| "Bid" sheet from order detail | `BidSheetView` | ☐ |

## Create Order

| iOS behavior | Source | Android |
|---|---|---|
| 27 equipment categories | `EquipmentCategory` | ☐ |
| Region + city + street + house number address | `CreateOrderView` | ☐ |
| Pricing unit dropdown (9 options) | `PricingUnit` | ☐ |
| Payment types multi-select | `PaymentType` | ☐ |
| Start date / time picker (default: tomorrow) | `CreateOrderViewModel.startDate` | ☐ |
| Bidding deadline (default: 1 hour before start) | `biddingDeadline` | ☐ |
| Duration hours (default 8) | `durationHours` | ☐ |
| Category-specific options (boom length, lift capacity, etc.) | `optBoomLength`, etc. | ☐ |
| Description with auto-prepended options summary | `buildOptionsSummary` | ☐ |
| Submit → `POST orders` | `OrdersService.createOrder` | ☐ |
| "Republish" flow seeding form from deleted order | `CreateOrderPrefill` | ☐ |

## My Orders / My Bids

| iOS behavior | Source | Android |
|---|---|---|
| `GET orders?view=mine` | `MyOrdersViewModel` | ☐ |
| `GET orders?view=pending` | `MyBidsViewModel` | ☐ |
| Customer accepts a bid → contact revealed | `acceptBid` returns `ContractorContact` | ☐ |
| Contractor withdraws their bid | `withdrawBid` | ☐ |
| Pagination 50/page on both | both view models | ☐ |
| Sign-in prompt when not authenticated | `SignInPromptView` | ☐ |

## Bidding

| iOS behavior | Source | Android |
|---|---|---|
| Load contractor's equipment, filtered by order category | `BidSheetViewModel.load` | ☐ |
| Block bid on own order | `BidSheetViewModel.submit` | ☐ |
| Inline "Add Equipment" sheet | `showingAddEquipment` | ☐ |
| Translate `DEPOSIT_REQUIRED` / 402 to user-friendly message | `BidSheetViewModel.submit` catch branch | ☐ |
| Submit → `POST orders/{id}/bids` | `OrdersService.submitBid` | ☐ |

## Garage

| iOS behavior | Source | Android |
|---|---|---|
| `GET equipment` listing | `EquipmentService.fetchEquipment` | ☐ |
| Add equipment with up to 4 photos | `AddEquipmentView` + `PhotosPicker` | ☐ |
| Concurrent photo uploads (`withThrowingTaskGroup`) | `AddEquipmentViewModel.submit` | ☐ |
| Deposit status badge per item (`paid` = green) | `Equipment.depositStatus` | ☐ |
| Edit equipment | `EditEquipmentView` | ☐ |
| Delete equipment | `EquipmentService.deleteEquipment` | ☐ |
| Performance security deposit flow (YooKassa) | `DepositService` + `DepositInfoSheet` | ☐ |
| Refund deposit | `DepositService.refundDeposit` | ☐ |
| Reload on `equipmentDidChange` event | observer in `GarageListViewModel` | ☐ |

## Profile

| iOS behavior | Source | Android |
|---|---|---|
| Show name, phone, email, city | `ProfileView` | ☐ |
| Edit profile → `PATCH users/me/profile` | `EditProfileView` + `ProfileAPI` | ☐ |
| Language toggle (EN/RU) | `LocalProfile.language` | ☐ AppCompatDelegate + per-app locale |
| Open Privacy Policy / Terms URLs | `openURL` | ☐ Intent.ACTION_VIEW |
| Logout | `SessionStore.clearSession` + `PushRegistrationService.unregister` | ☐ |

## News

| iOS behavior | Source | Android |
|---|---|---|
| `GET news` (no auth) | `NewsStore.fetch` | ☐ |
| Image + optional video URL per item | `NewsItem` | ☐ |
| Pull to refresh | iOS-default | ☐ |

## Notifications

| iOS behavior | Source | Android |
|---|---|---|
| Register APNs token after login → `POST notifications/register` | `PushRegistrationService.registerIfNeeded` | ☐ FCM token |
| Unregister on logout | `PushRegistrationService.unregister` | ☐ |
| Persist last 100 notifications in UserDefaults | `NotificationStore` | ☐ DataStore |
| Mark read / mark all read / clear | `NotificationStore` | ☐ |
| Unread badge on bell icon | `notificationStore.unreadCount` | ☐ |
| Tap notification → navigate to correct tab + push order detail | `MainTabView.handleNotificationNavigation` | ☐ |
| Foreground delivery shows banner + sound | `userNotificationCenter(_:willPresent:)` | ☐ Notification with priority HIGH |
| Deep-link types: `new_bid`, `bid_accepted`, `matching_order`, etc. | `notificationTarget(for:)` | ☐ |

## Support chat

| iOS behavior | Source | Android |
|---|---|---|
| Send a message (optionally with order id) → `POST support/messages` | `SupportChatViewModel.send` | ☐ |
| No-auth allowed | `SupportAPI.requiresAuth = false` | ☐ |

## Saved filters & opt-in notifications

| iOS behavior | Source | Android |
|---|---|---|
| GET/PUT/DELETE `notifications/saved-filter` | `SavedFilterAPI` | ☐ |
| Toggle enabled → `POST notifications/saved-filter/enabled` | `SavedFilterStore.setNotificationsEnabled` | ☐ |
| Local mirror in UserDefaults | `SavedFilterStore` | ☐ DataStore |
| Request push permission before enabling | `requestAuthorization` | ☐ `POST_NOTIFICATIONS` runtime permission |

## Infrastructure / non-functional

| iOS behavior | Source | Android |
|---|---|---|
| HMAC-SHA256 request signing (X-Client-Id, X-Timestamp, X-Nonce, X-Signature) | `APIClient.signRequest` | ☐ |
| Cert pinning on `spectech-backoffice.onrender.com` (two intermediates) | `APIClient.PinningDelegate` | ☐ OkHttp `CertificatePinner` |
| Retry 502/503/504 up to 2x with exponential delay | `APIClient.executeRequest` | ☐ |
| Snake_case ↔ camelCase JSON conversion | `JSONDecoder.convertFromSnakeCase` | ☐ kotlinx-serialization `JsonNamingStrategy.SnakeCase` |
| ISO-8601 with fractional seconds date parsing | `ISO8601DateFormatter.full` | ☐ |
| Cancel-safe `RemoteState<T>` UI driver | `RemoteState` enum | ☐ sealed class |
