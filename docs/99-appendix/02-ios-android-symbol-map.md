# 02 — iOS ↔ Android Symbol Map

A bidirectional dictionary for navigating between the iOS source and the
Android port.

## App entry & lifecycle

| iOS | Android |
|---|---|
| `SpecTechIOSApp` (`@main App`) | `SpecTechApplication` + `MainActivity` |
| `@UIApplicationDelegateAdaptor(AppDelegate.self)` | `Application.onCreate` |
| `@State private var sessionStore = SessionStore()` (root) | Hilt `@Singleton` `SessionStore` |
| `RootView` | `MainActivity setContent { … }` |
| `RootRouterView` | `SpecTechApp()` composable + `NavHost` |
| `WindowGroup` + `.sheet` | `setContent { … }` + `ModalBottomSheet` |
| `@UIApplicationDelegateAdaptor` AppDelegate | `Application` hooks + manifest-registered services |

## DI / state

| iOS | Android |
|---|---|
| `@Observable` class | `class : ViewModel` + `MutableStateFlow` / `mutableStateOf` |
| `@MainActor` | `viewModelScope` (defaults to Main.immediate) |
| `@Environment(SomeStore.self)` | `hiltViewModel<…>()` for VMs, `@Inject` for repos |
| `@StateObject` / `@State` for VMs | `viewModel: VM = hiltViewModel()` |
| `@Binding var x: Type` | callback `(Type) -> Unit` + state in parent |

## Navigation

| iOS | Android |
|---|---|
| `NavigationStack(path: $path)` | `NavHost(controller, startDestination = …)` |
| `NavigationPath` | `NavController` |
| `path.append(item)` | `navController.navigate(route)` |
| `.navigationDestination(for: Order.self)` | `composable<OrderDetailRoute> { … }` |
| `.sheet(isPresented:)` | `ModalBottomSheet(onDismissRequest:)` |
| `.fullScreenCover` | full-screen `composable<…>` destination |
| Tab `TabView` + `Tab(…)` | `NavigationBar` + `NavigationBarItem` (nested NavHosts per tab) |
| `.navigationTitle("…")` | `TopAppBar(title = { Text(…) })` |
| `.toolbar { ToolbarItem … }` | `TopAppBar(actions = { … }, navigationIcon = { … })` |

## Networking

| iOS | Android |
|---|---|
| `URLSession` + custom `APIClient` | `HttpClient(OkHttp)` (Ktor) |
| `URLSessionDelegate` for pinning | `OkHttpClient.certificatePinner` or custom `TrustManager` |
| `URLRequest.setValue(…, forHTTPHeaderField:)` | Ktor `request.header(…)` |
| HMAC done before `data(for:)` | OkHttp interceptor (`HmacInterceptor`) |
| `CryptoKit` `HMAC<SHA256>` | `javax.crypto.Mac` `"HmacSHA256"` |
| `SHA256.hash(data:)` | `MessageDigest.getInstance("SHA-256")` |
| `SecRandomCopyBytes` | `java.security.SecureRandom` |
| `JSONDecoder` `.convertFromSnakeCase` | kotlinx `JsonNamingStrategy.SnakeCase` |
| `JSONEncoder` `.withoutEscapingSlashes` | kotlinx-serialization default (no `\/` escaping) |
| `URLError.cancelled` | `CancellationException` |
| `Task.sleep(nanoseconds: 2_000_000_000)` | `delay(2.seconds)` |

## Storage

| iOS | Android |
|---|---|
| Keychain `SecItemAdd/Update/CopyMatching` | `EncryptedSharedPreferences` |
| `UserDefaults` | Preferences `DataStore` |
| `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` | `MasterKey.KeyScheme.AES256_GCM` |

## UI primitives

| iOS | Android |
|---|---|
| `VStack` | `Column` |
| `HStack` | `Row` |
| `ZStack` | `Box` |
| `LazyVStack` (in `ScrollView`) | `LazyColumn` |
| `ScrollView` | `Modifier.verticalScroll(rememberScrollState())` |
| `List` | `LazyColumn` with `items(…)` |
| `Spacer()` | `Spacer(Modifier.weight(1f))` or `Modifier.height(…)` |
| `Image(systemName: "bell")` | `Icon(Icons.Outlined.Notifications, null)` |
| `Text("…")` | `Text("…")` |
| `Button(action:) { … }` | `Button(onClick = …) { … }` |
| `Capsule()` | `RoundedCornerShape(percent = 50)` |
| `TextField("placeholder", text: $value)` | `TextField(value, onValueChange = { value = it })` |
| `.foregroundStyle(.red)` | `color = MaterialTheme.colorScheme.error` |
| `.font(.system(size: 17, weight: .semibold))` | `style = MaterialTheme.typography.titleMedium.copy(…)` |
| `.padding(.horizontal, 16)` | `.padding(horizontal = 16.dp)` |
| `.frame(maxWidth: .infinity)` | `.fillMaxWidth()` |
| `.background(Color.blue)` | `.background(BrandBlue)` |
| `.cornerRadius(12)` | `.clip(RoundedCornerShape(12.dp))` |
| `Color(.systemBackground)` | `MaterialTheme.colorScheme.surface` |
| `Color(.systemGray6)` | `MaterialTheme.colorScheme.surfaceVariant` |
| `ProgressView()` | `CircularProgressIndicator()` |
| `Alert` / `confirmationDialog` | `AlertDialog` |
| `Picker` | `DropdownMenu` / `ExposedDropdownMenuBox` |
| `DatePicker` | `DatePickerDialog` (Material 3) |
| `Toggle` | `Switch` |
| `PhotosPicker` | `rememberLauncherForActivityResult(PickMultipleVisualMedia)` |

## Concurrency

| iOS | Android |
|---|---|
| `async/await` | `suspend fun` |
| `try await foo()` | `foo()` (already suspends) |
| `Task { … }` | `viewModelScope.launch { … }` |
| `Task.detached` | `applicationScope.launch { … }` |
| `withThrowingTaskGroup` | `coroutineScope { … awaitAll() }` |
| `Task.sleep(nanoseconds:)` | `delay(…)` |
| `CancellationError` | `CancellationException` |

## Domain types

| iOS | Android |
|---|---|
| `UUID` | `kotlin.uuid.Uuid` |
| `Decimal` | `BigDecimal` |
| `Date` | `kotlinx.datetime.Instant` |
| `String(localized: "…")` | `stringResource(R.string.…)` |
| `Codable` | `@Serializable` (kotlinx-serialization) |
| `LocalizedStringKey` | `@StringRes Int` |

## Push notifications

| iOS | Android |
|---|---|
| APNs device token (binary) | FCM token (string) |
| `UNUserNotificationCenter.delegate` | `FirebaseMessagingService` |
| `UNUserNotificationCenter.willPresent` | `FcmService.onMessageReceived` (foreground) |
| `UNUserNotificationCenter.didReceive` | `MainActivity.onNewIntent` (after tap) |
| `userInfo["aps"]["alert"]` | `RemoteMessage.notification.title/body` + `RemoteMessage.data` |
| `UIApplication.shared.registerForRemoteNotifications()` | `FirebaseMessaging.getInstance().token` |

## Image handling

| iOS | Android |
|---|---|
| `UIImage` | `Bitmap` |
| `UIImage.jpegData(compressionQuality:)` | `Bitmap.compress(JPEG, quality, out)` |
| `UIGraphicsImageRenderer` | `Bitmap.createScaledBitmap` |
| `SDWebImageSwiftUI WebImage` | `coil.compose.AsyncImage` |
| `SDImageCache.shared.config` | `ImageLoader.Builder().memoryCache/diskCache` |
| `SFSafariViewController` | `androidx.browser.CustomTabsIntent` |

## Useful one-liners

```
iOS:      AuthService(sessionStore: sessionStore)
Android:  @Inject constructor(private val sessionStore: SessionStore)

iOS:      Task { await viewModel.load() }
Android:  viewModel.load()  // method is suspend or launches its own job

iOS:      .onAppear { Task { await load() } }
Android:  LaunchedEffect(Unit) { load() }

iOS:      .onChange(of: filter) { _, _ in Task { await load() } }
Android:  LaunchedEffect(filter) { load() }
```
