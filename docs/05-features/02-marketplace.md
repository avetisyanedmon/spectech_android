# 02 — Marketplace

iOS sources:
- `SpecTechIOS/Scene/Tabs/Marketplace/MarketplaceListViewModel.swift`
- `SpecTechIOS/Scene/Tabs/Marketplace/MarketplaceListView.swift`
- `SpecTechIOS/Scene/Tabs/Marketplace/Filters/MarketplaceFilterSheet.swift`
- `SpecTechIOS/Scene/Tabs/Marketplace/Order/OrderCardView.swift`
- `SpecTechIOS/Scene/Tabs/Marketplace/Order/OrderDetailView.swift`

## Behavior

- **Public** read: `GET /orders?view=marketplace` does NOT require auth.
- Client filters out **expired** orders (`Order.isExpired`) and statuses
  other than `open`.
- Per-page size: 50 (`OrdersService.defaultPageSize`).
- Infinite scroll: when the **last** rendered item enters viewport, fetch the
  next page using current `offset`.
- Reloads on `DomainEvent.OrdersChanged` (replaces iOS `ordersDidChange`).
- Reloads when filters change.
- Reloads when the current user id changes (sign-in / sign-out).

## ViewModel

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

    var showingFilters by mutableStateOf(false)
    var isLoadingMore by mutableStateOf(false); private set
    var hasMorePages by mutableStateOf(true); private set
    private var currentOffset = 0
    private val pageSize = OrdersRepository.PAGE_SIZE

    val visibleOrders: StateFlow<List<Order>> = combine(state, filters) { s, f ->
        when (s) {
            is RemoteState.Loaded -> s.value.filter { it.status == OrderStatus.OPEN && !it.isExpired }
                .filter(f::matches)
            else -> emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val canCreateBid: Boolean get() = sessionStore.isAuthenticated

    init {
        viewModelScope.launch {
            events.events.filterIsInstance<DomainEvent.OrdersChanged>().collect {
                load(forceRefresh = true)
            }
        }
        viewModelScope.launch {
            sessionStore.currentSession.map { it?.user?.id }.distinctUntilChanged().drop(1).collect {
                load(forceRefresh = true)
            }
        }
        viewModelScope.launch {
            _filters.drop(1).collect { load(forceRefresh = true) }
        }
    }

    fun load(forceRefresh: Boolean = false) = viewModelScope.launch {
        if (!forceRefresh) _state.value = RemoteState.Loading
        currentOffset = 0
        hasMorePages = true
        try {
            val orders = ordersRepo.fetchOrders(OrderScope.MARKETPLACE, _filters.value)
            currentOffset = orders.size
            hasMorePages = orders.size >= pageSize
            _state.value = if (orders.isEmpty())
                RemoteState.Empty(R.string.marketplace_empty)
            else
                RemoteState.Loaded(orders)
        } catch (e: CancellationException) { throw e }
        catch (e: ApiError) { _state.value = RemoteState.Failed(e) }
        catch (e: Exception) { _state.value = RemoteState.Failed(ApiError.from(e)) }
    }

    fun loadMoreIfNeeded(currentItem: Order) = viewModelScope.launch {
        val visible = visibleOrders.value
        if (!hasMorePages || isLoadingMore || visible.isEmpty()) return@launch
        val last = visible.last()
        if (currentItem.id != last.id) return@launch

        isLoadingMore = true
        try {
            val next = ordersRepo.fetchOrdersPage(OrderScope.MARKETPLACE, pageSize, currentOffset, _filters.value)
            hasMorePages = next.size >= pageSize
            currentOffset += next.size
            (_state.value as? RemoteState.Loaded<List<Order>>)?.let { loaded ->
                val seen = loaded.value.map { it.id }.toSet()
                val deduped = next.filterNot { it.id in seen }
                _state.value = RemoteState.Loaded(loaded.value + deduped)
            }
        } catch (_: Exception) {
            hasMorePages = false
        } finally {
            isLoadingMore = false
        }
    }

    fun setFilters(new: OrderFilters) { _filters.value = new }
}
```

## Screen layout (Compose)

```kotlin
@Composable
fun MarketplaceScreen(
    onOrderClick: (Order) -> Unit,
    onCreateOrderClick: () -> Unit,
    onSignInClick: () -> Unit,
    viewModel: MarketplaceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val visible by viewModel.visibleOrders.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { if (state is RemoteState.Idle) viewModel.load() }

    Scaffold(
        topBar = { MarketplaceTopBar(
            onFiltersClick = { viewModel.showingFilters = true },
            onCreateOrderClick = onCreateOrderClick,
            isAuthenticated = viewModel.canCreateBid,
            onSignInClick = onSignInClick,
        )}
    ) { padding ->
        when (state) {
            RemoteState.Idle, RemoteState.Loading -> LoadingStateView(R.string.loading_marketplace, padding)
            is RemoteState.Empty -> EmptyStateView(R.string.no_orders, onAction = { viewModel.load(true) })
            is RemoteState.Failed -> ErrorStateView((state as RemoteState.Failed).error, onRetry = { viewModel.load(true) })
            is RemoteState.Loaded -> OrderList(
                orders = visible,
                onItemClick = onOrderClick,
                onLastItemVisible = { viewModel.loadMoreIfNeeded(it) },
            )
        }
    }

    if (viewModel.showingFilters) {
        MarketplaceFilterSheet(
            filters = viewModel.filters.collectAsStateWithLifecycle().value,
            onApply = { viewModel.setFilters(it); viewModel.showingFilters = false },
            onDismiss = { viewModel.showingFilters = false },
        )
    }
}
```

`OrderList` uses `LazyColumn` with `key = it.id`. `onLastItemVisible` fires
when the last visible index reaches the last item — equivalent of iOS's
`.onAppear { loadMoreIfNeeded(currentItem: order) }` per cell.

## FilterSheet structure

Five sections — each is a multi-select chip group or autocomplete:

| Section | Source |
|---|---|
| Equipment category | 27 categories, scrollable chips |
| Region | free-text multi-add (regions of Russia) |
| City | autocomplete (Google Places) multi-add |
| Pricing unit | 9 units, chips |
| Payment type | 3 types, chips |

Apply button writes the new filters via `viewModel.setFilters(...)` and
closes the sheet — the VM's `filters` flow then triggers `load(true)`.

The iOS sheet also has a button to **Save filter for notifications**, which
hands off to `SavedFilterStore.save(...)`. See [13-saved-filters.md](13-saved-filters.md).

## OrderCardView

Each row shows:
- Equipment category title + status badge (`OrderStatus.titleRes` colored
  per [06-ui/06-status-and-category-mapping.md](../06-ui/06-status-and-category-mapping.md))
- Address (`Order.displayAddress`)
- Pricing unit + payment types (chips)
- Start date and duration
- Bid count badge
- Creator name (if marketplace view)

## OrderDetailView

Top section: same as card but expanded. Below: list of submitted bids.

- For the **customer's own order**: each bid shows the contractor's price,
  equipment, comment, and an "Accept" button. Accepting reveals the
  contractor's phone (returned by `acceptBid`).
- For a **contractor** (marketplace view): show only their own submitted
  bid if any; otherwise show a CTA "Submit a bid" that opens the bid sheet.
- For an **unauthenticated** user: order info is visible; "Submit a bid"
  CTA opens the auth sheet first.

Photo viewers for bid equipment photos use Coil's `AsyncImage`:

```kotlin
AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop)
```

Tap a photo → open full-screen `Dialog` with pinch-zoom (use a Compose
zoomable modifier).
