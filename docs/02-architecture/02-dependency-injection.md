# 02 — Dependency Injection

## What iOS does

iOS creates a handful of stateful objects once (in `SpecTechIOSApp`) and
injects them via SwiftUI's environment:

```swift
@State private var sessionStore = SessionStore()
@State private var profileStore = ProfileStore()
@State private var newsStore = NewsStore()
@State private var notificationStore = NotificationStore()
@State private var savedFilterStore = SavedFilterStore()

RootView()
  .environment(sessionStore)
  .environment(profileStore)
  // …
```

Views then read them with `@Environment(SessionStore.self)`.

Service classes (`AuthService`, `OrdersService`, `EquipmentService`,
`DepositService`) are **not** kept in the environment — they are constructed
on demand inside view models with `APIClient(sessionProvider: sessionStore)`.

## Android equivalent: Hilt

Hilt gives us the same model with compile-time safety:

- **Singletons** (one per app) → `@Singleton` `@Module` `@InstallIn(SingletonComponent::class)`
- **Service / repository instances** (cheap to construct, transient) →
  `@Inject constructor(...)` on the class itself, no module needed
- **ViewModels** → `@HiltViewModel class FooViewModel @Inject constructor(...)`
- **Screen access** → `hiltViewModel<FooViewModel>()` in Compose

## The graph

### Singletons

```kotlin
// core/platform/di/PlatformModule.kt
@Module
@InstallIn(SingletonComponent::class)
object PlatformModule {

    @Provides @Singleton
    fun provideSecureStorage(@ApplicationContext ctx: Context): SecureStorage =
        EncryptedSecureStorage(ctx)

    @Provides @Singleton
    fun provideAppConfiguration(): AppConfiguration = AppConfiguration.load()
}

// core/network/di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideHttpClient(
        config: AppConfiguration,
        sessionProvider: SessionProvider,
    ): HttpClient = buildKtorClient(config, sessionProvider)

    @Provides @Singleton
    fun provideApiClient(client: HttpClient): ApiClient = ApiClientImpl(client)
}

// core/data/di/DataModule.kt — Stores live here because they're stateful (Observable)
@Module
@InstallIn(SingletonComponent::class)
object StoresModule {
    @Provides @Singleton fun sessionStore(secure: SecureStorage): SessionStore =
        SessionStore(secure)

    @Provides @Singleton fun profileStore(secure: SecureStorage): ProfileStore =
        ProfileStore(secure)

    @Provides @Singleton fun notificationStore(@ApplicationContext ctx: Context): NotificationStore =
        NotificationStore(ctx)

    @Provides @Singleton fun newsStore(api: ApiClient): NewsStore = NewsStore(api)

    @Provides @Singleton fun savedFilterStore(@ApplicationContext ctx: Context, api: ApiClient): SavedFilterStore =
        SavedFilterStore(ctx, api)
}
```

### SessionProvider circular dependency

`ApiClient` needs `SessionStore` (to pull the token) **and** `SessionStore`
needs `ApiClient` for nothing in iOS (it just persists). On Android we keep
this clean: `SessionStore` only knows how to **provide** a token and **clear**
itself — it does **not** depend on the HTTP client.

```kotlin
// core/network/SessionProvider.kt
interface SessionProvider {
    suspend fun authToken(): String?
    suspend fun clearSession()
}

// core/data/SessionStore.kt implements SessionProvider
```

Bind via Hilt:

```kotlin
@Module @InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds @Singleton
    abstract fun bindSessionProvider(impl: SessionStore): SessionProvider
}
```

This is identical to the iOS pattern where `SessionStore` conforms to
`SessionProviding` and is passed into `APIClient(sessionProvider: …)`.

### Repositories (constructor inject only)

```kotlin
// core/data/orders/OrdersRepository.kt
class OrdersRepository @Inject constructor(
    private val api: ApiClient,
    private val events: AppEventBus,
) {
    suspend fun fetchOrders(scope: OrderScope, filters: OrderFilters? = null): List<Order> { … }
    suspend fun createOrder(req: CreateOrderRequest): Order {
        val o = api.send(OrdersApi.CreateOrder(req))
        events.emit(DomainEvent.OrdersChanged)
        return o
    }
    // …
}
```

`OrdersRepository`, `EquipmentRepository`, `AuthRepository`, `DepositRepository`,
`PushRepository`, `ProfileRepository`, `SupportRepository`, `SavedFilterRepository`,
`NewsRepository` all follow this pattern. No module needed — Hilt sees the
`@Inject constructor` and provides them automatically.

### ViewModels

```kotlin
@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val ordersRepo: OrdersRepository,
    private val sessionStore: SessionStore,
    private val events: AppEventBus,
) : ViewModel() {
    private val _state = MutableStateFlow<RemoteState<List<Order>>>(RemoteState.Idle)
    val state: StateFlow<RemoteState<List<Order>>> = _state.asStateFlow()
    // …
}
```

Screen:

```kotlin
@Composable
fun MarketplaceScreen(viewModel: MarketplaceViewModel = hiltViewModel()) { … }
```

## Mapping from iOS

| iOS pattern | Android equivalent |
|---|---|
| `@State private var sessionStore = SessionStore()` at app root | `@Provides @Singleton SessionStore` in Hilt module |
| `.environment(sessionStore)` | Constructor inject into ViewModel / Composable's VM |
| `@Environment(SessionStore.self) private var sessionStore` in a View | `viewModel.sessionState` exposed as `StateFlow` |
| `init() { self.apiClient = APIClient(sessionProvider: sessionStore) }` inside a service | `@Inject constructor(private val api: ApiClient)` |
| `@Observable` class | `class … : ViewModel()` (and don't extend if shared across screens — keep as Hilt singleton) |
| `@MainActor` on a class | Coroutines launched in `viewModelScope` already use `Dispatchers.Main.immediate` |

## App-wide event bus (replaces `NotificationCenter`)

```kotlin
// core/data/events/AppEventBus.kt
sealed interface DomainEvent {
    data object OrdersChanged : DomainEvent
    data object EquipmentChanged : DomainEvent
    data class DeviceTokenReceived(val token: String) : DomainEvent
}

@Singleton
class AppEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<DomainEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<DomainEvent> = _events.asSharedFlow()
    suspend fun emit(event: DomainEvent) = _events.emit(event)
}
```

This replaces:
- `Notification.Name.ordersDidChange` (`OrdersService.notifyOrdersChanged`)
- `Notification.Name.equipmentDidChange` (`EquipmentService.notifyEquipmentChanged`)
- `Notification.Name.didReceiveDeviceToken` (`AppDelegate`)

ViewModels collect from this in `init`:

```kotlin
init {
    viewModelScope.launch {
        events.events.collect { event ->
            if (event is DomainEvent.OrdersChanged) refresh(forceRefresh = true)
        }
    }
}
```

This is a direct port of the `NotificationCenter` observer pattern used by
`MarketplaceListViewModel`, `MyOrdersViewModel`, `MyBidsViewModel`, and
`GarageListViewModel`.
