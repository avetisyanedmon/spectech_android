# 04 — State Management

## iOS pattern

Every view model and shared store is `@Observable` + `@MainActor`:

```swift
@MainActor
@Observable
final class MarketplaceListViewModel {
    var state: RemoteState<[Order]> = .idle
    var filters = OrderFilters()
    var showingFilters = false
    // …
    func load(forceRefresh: Bool = false) async { … }
}
```

Views read state directly (`viewModel.state`) and SwiftUI tracks reads
automatically.

## Android pattern

Use `ViewModel` + `StateFlow` for surfaces that survive configuration changes,
and `mutableStateOf` for purely screen-local state.

### The 1:1 mapping

```kotlin
@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val ordersRepo: OrdersRepository,
    private val sessionStore: SessionStore,
    private val events: AppEventBus,
) : ViewModel() {

    private val _state = MutableStateFlow<RemoteState<List<Order>>>(RemoteState.Idle)
    val state: StateFlow<RemoteState<List<Order>>> = _state.asStateFlow()

    private val _filters = MutableStateFlow(OrderFilters())
    val filters: StateFlow<OrderFilters> = _filters.asStateFlow()

    var showingFilters by mutableStateOf(false)   // pure UI toggle — no need to persist across config

    private var currentOffset = 0
    var isLoadingMore by mutableStateOf(false)
        private set
    var hasMorePages by mutableStateOf(true)
        private set

    init {
        viewModelScope.launch {
            events.events.filterIsInstance<DomainEvent.OrdersChanged>().collect {
                load(forceRefresh = true)
            }
        }
        // Optionally also collect filters and reload on change
        viewModelScope.launch {
            _filters.drop(1).collect { load(forceRefresh = true) }
        }
    }

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh) _state.value = RemoteState.Loading
            currentOffset = 0
            hasMorePages = true
            runCatching { ordersRepo.fetchOrders(OrderScope.MARKETPLACE, _filters.value) }
                .onSuccess { orders ->
                    currentOffset = orders.size
                    hasMorePages = orders.size >= OrdersRepository.PAGE_SIZE
                    _state.value =
                        if (orders.isEmpty()) RemoteState.Empty(R.string.marketplace_empty)
                        else RemoteState.Loaded(orders)
                }
                .onFailure { e ->
                    _state.value = RemoteState.Failed(ApiError.from(e))
                }
        }
    }

    fun setFilters(new: OrderFilters) { _filters.value = new }
}
```

### Composable consumption

```kotlin
@Composable
fun MarketplaceScreen(viewModel: MarketplaceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val pull = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        if (state is RemoteState.Idle) viewModel.load()
    }

    when (state) {
        RemoteState.Idle, RemoteState.Loading -> LoadingStateView(R.string.loading_marketplace)
        is RemoteState.Empty -> EmptyStateView(/* … */)
        is RemoteState.Failed -> ErrorStateView(/* … */)
        is RemoteState.Loaded -> MarketplaceContent(state.value)
    }
}
```

`collectAsStateWithLifecycle()` is the lifecycle-aware collector — it cancels
collection in STOPPED state (mirrors iOS's automatic unsubscribe when the view
disappears).

### When to use `mutableStateOf` vs `StateFlow`

| Case | Use |
|---|---|
| Data fetched from network | `StateFlow` |
| State driven by events from other VMs / repos | `StateFlow` |
| Sheet-open / dropdown-open / focus / scroll position | `mutableStateOf` inside the VM (or `rememberSaveable` in the composable) |
| Pure form input (text field) | `mutableStateOf` inside the VM |
| Anything other VMs/repositories need to observe | `StateFlow` |

Note: in iOS, both kinds become `var` properties on the `@Observable` class.
Android forces the distinction more sharply, but the rule of thumb is "if some
other thing needs to react to the change, make it a `StateFlow`."

## RemoteState sealed class

Replaces `RemoteState<Value>` enum:

```kotlin
// core/ui-kit/state/RemoteState.kt
sealed interface RemoteState<out T> {
    data object Idle : RemoteState<Nothing>
    data object Loading : RemoteState<Nothing>
    data class Loaded<T>(val value: T) : RemoteState<T>
    data class Empty(@StringRes val messageRes: Int) : RemoteState<Nothing>
    data class Failed(val error: ApiError) : RemoteState<Nothing>
}
```

`@StringRes` so messages can be localized via Android resources rather than
hard-coded strings.

## Persisted state

State that must survive process death goes into:

| Use case | iOS | Android |
|---|---|---|
| Auth session | Keychain | EncryptedSharedPreferences (see [07-infrastructure/01-secure-storage.md](../07-infrastructure/01-secure-storage.md)) |
| Local profile | Keychain (`local_profile`) | EncryptedSharedPreferences |
| Saved filters & notification opt-in | UserDefaults | Preferences DataStore |
| Notification history (last 100) | UserDefaults | Preferences DataStore (JSON-encoded list) |
| Withdrawn bids cache | UserDefaults | Preferences DataStore |

All of these are wrapped behind a thin Kotlin facade so the ViewModel doesn't
need to know which storage backs which key.
