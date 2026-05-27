# 02 — Tech Stack

The choices below mirror iOS capabilities one-to-one. Where Android offers
multiple equivalents, the recommended option is listed first; alternates are
called out only when there's a real trade-off.

| Concern | iOS | Android (chosen) | Why |
|---|---|---|---|
| Language | Swift 5.10 | **Kotlin 2.0** | Match feature parity (sealed classes, coroutines) |
| Min platform | iOS 17 | **Android 8.0 / API 26** | EncryptedSharedPreferences requires 23+; we go 26 for `java.time` and FCM modern features |
| UI toolkit | SwiftUI | **Jetpack Compose** | Declarative parity with SwiftUI |
| Design system | Custom (brand blue, system grays) | **Material 3** + custom tokens | M3 covers list/sheet/tab; custom palette overrides where needed |
| State | `@Observable` macro | **ViewModel + StateFlow** | `StateFlow` is the idiomatic equivalent; `mutableStateOf` for screen-local state |
| Concurrency | `async/await` + `@MainActor` | **Coroutines + `viewModelScope` + `Dispatchers.Main.immediate`** | Direct 1:1 |
| DI | SwiftUI `@Environment` + plain singletons | **Hilt** | Closest match to "inject by environment" |
| Navigation | `NavigationStack(path:)` + `.navigationDestination(for:)` | **Navigation Compose with type-safe routes** (`@Serializable` route classes) | Same per-tab back stack model |
| HTTP client | `URLSession` + custom `APIClient` | **Ktor `HttpClient` (engine OkHttp)** | Native coroutine API + OkHttp gives us `CertificatePinner` |
| JSON | `JSONDecoder`/`JSONEncoder` | **kotlinx.serialization** | Snake-case strategy, custom serializers map directly |
| Date handling | `ISO8601DateFormatter` | **`kotlinx-datetime` Instant** | ISO-8601 + fractional seconds parser |
| Secure storage | Keychain | **EncryptedSharedPreferences** (Jetpack Security) | Same threat model (per-device, hardware-backed where available) |
| Reactive cache events | `NotificationCenter` | **`SharedFlow<DomainEvent>`** in a singleton `EventBus` | Same pub/sub for "orders changed" / "equipment changed" |
| Image loading | SDWebImageSwiftUI | **Coil 2.x** | Caching strategy parity; configurable memory/disk caps |
| Image picker | `PhotosPicker` | **`ActivityResultContracts.PickMultipleVisualMedia`** | Photo Picker is the supported modern API |
| Image encoding | UIKit + `ImageEncoder.jpegData` | **`Bitmap.compress(JPEG, ...)`** with custom resize loop | See [07/03-image-encoding-upload.md](../07-infrastructure/03-image-encoding-upload.md) |
| Crypto (HMAC-SHA256, SHA-256) | `CryptoKit` | **`javax.crypto.Mac` + `java.security.MessageDigest`** | Bundled with JDK |
| Phone formatter | Custom `RussianPhoneFormatter` | **Port verbatim to Kotlin** | Logic is small and locale-specific — no library needed |
| Push notifications | APNs via `UIApplicationDelegate` | **Firebase Cloud Messaging (FirebaseMessagingService)** | Backend already sends `platform: "ios"`; add `platform: "android"` |
| City autocomplete | MapKit `MKLocalSearchCompleter` | **Google Places SDK** (Autocomplete) | Equivalent UX; needs Places API key |
| Web view for YooKassa | `SFSafariViewController` | **Chrome Custom Tabs** | YooKassa returns to deep-link callback in both cases |
| Logging | `os.Logger` (DEBUG only) | **Timber** with debug-only `Tree` | Same DEBUG-gating approach |
| Testing | XCTest | **JUnit 5 + Turbine + Mockk** (unit); **Compose UI Test** (instrumentation) | Standard Android stack |

## Why not these alternatives

- **Retrofit instead of Ktor?** Retrofit needs an OkHttp interceptor for the
  HMAC headers anyway, and Ktor's `HttpClient` is more ergonomic for the
  multipart photo upload + custom serialization. Either works. If the team is
  already comfortable with Retrofit, use Retrofit + OkHttp interceptor — but
  HMAC signing must execute **after** the body is finalized (see
  [04-networking/02-hmac-signing.md](../04-networking/02-hmac-signing.md)).
- **Koin instead of Hilt?** Hilt's compile-time graph catches errors early,
  matches the static nature of the iOS environment graph, and is the AndroidX
  recommendation. Koin is simpler but trades safety for flexibility.
- **Moshi instead of kotlinx.serialization?** Moshi works but ksp-based
  kotlinx.serialization fits multiplatform-style sealed/enum serialization
  better (e.g. tolerant `OrderStatus` decoder).
- **Glide instead of Coil?** Coil is Kotlin-first, smaller, and integrates
  with Compose natively (`AsyncImage`).

## Required external accounts / keys

- **Backend client secret** — already embedded in `AppConfiguration.swift`;
  port to `BuildConfig` (see [07/04-app-configuration.md](../07-infrastructure/04-app-configuration.md)).
- **FCM project + `google-services.json`** — new Firebase project; backend
  must add Android-side push handling (sends should set `platform: "android"`).
- **Google Places API key** — for the city autocomplete field.
- **Play Store signing key** — see [08-testing-build/04-release.md](../08-testing-build/04-release.md).
