# 05 — My Bids

iOS sources:
- `SpecTechIOS/Scene/Tabs/MyBids/MyBidsViewModel.swift`
- `SpecTechIOS/Scene/Tabs/MyBids/MyBidsView.swift`
- `SpecTechIOS/Scene/Tabs/MyBids/Components/MyBidCardView.swift`
- `SpecTechIOS/Scene/Tabs/MyBids/Components/WithdrawBidButton.swift`

The contractor's view of orders they've placed a bid on.

## Endpoint

`GET /orders?view=pending` (auth required, paged 50/page).

The backend returns each order whose bid list contains a bid from the
current contractor. Each order has its `bids` array, and the contractor's
own bid is the one shown in the card.

## ViewModel

```kotlin
@HiltViewModel
class MyBidsViewModel @Inject constructor(
    private val ordersRepo: OrdersRepository,
    private val events: AppEventBus,
) : ViewModel() {

    private val _state = MutableStateFlow<RemoteState<List<Order>>>(RemoteState.Idle)
    val state: StateFlow<RemoteState<List<Order>>> = _state.asStateFlow()
    var withdrawError by mutableStateOf<ApiError?>(null)
    var isLoadingMore by mutableStateOf(false); private set
    var hasMorePages by mutableStateOf(true); private set
    private var currentOffset = 0

    init {
        viewModelScope.launch {
            events.events.filterIsInstance<DomainEvent.OrdersChanged>().collect {
                load(forceRefresh = true)
            }
        }
    }

    fun load(forceRefresh: Boolean = false) = viewModelScope.launch { /* identical to others */ }
    fun loadMoreIfNeeded(item: Order) = viewModelScope.launch { /* identical */ }

    fun withdrawBid(orderId: Uuid, bidId: Uuid) = viewModelScope.launch {
        runCatching { ordersRepo.withdrawBid(orderId, bidId) }
            .onSuccess { load(forceRefresh = true) }
            .onFailure { withdrawError = ApiError.from(it) }
    }
}
```

## Withdraw button

iOS `WithdrawBidButton.swift` shows a destructive button with confirmation.

```kotlin
@Composable
fun WithdrawBidButton(orderId: Uuid, bidId: Uuid, onWithdraw: (Uuid, Uuid) -> Unit) {
    var showingConfirm by remember { mutableStateOf(false) }
    Button(
        onClick = { showingConfirm = true },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
    ) { Text(stringResource(R.string.withdraw_bid)) }

    if (showingConfirm) {
        AlertDialog(
            onDismissRequest = { showingConfirm = false },
            title = { Text(stringResource(R.string.withdraw_bid_confirm_title)) },
            text = { Text(stringResource(R.string.withdraw_bid_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showingConfirm = false
                    onWithdraw(orderId, bidId)
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { showingConfirm = false }) {
                Text(stringResource(R.string.cancel))
            }},
        )
    }
}
```

## MyBidCardView contents

Each card shows the contractor's bid on a given order:
- Order's equipment category title + status badge
- Order address
- The contractor's bid price + delivery price
- Payment type
- Bid status:
  - "Pending" — bid submitted, customer not yet accepted
  - "Accepted" — `Order.acceptedBidId == thisBid.id` → reveal customer's
    contact (the order's `creatorPhone` and `creatorName`)
  - "Rejected" — customer accepted a different bid
  - "Withdrawn" — local-only state after the user taps withdraw

Tap the card → open the order detail screen, which shows the same content
with more bids context (other contractors' bids hidden — only the current
user's own bid is visible).

## Withdrawn bids cache

iOS maintains a `MyBidsCacheStore` for withdrawn bids so the UI can mark
them locally before the next refresh. Mirror with a small DataStore-backed
set of `bidId`s:

```kotlin
@Singleton
class WithdrawnBidsCache @Inject constructor(@ApplicationContext ctx: Context) {
    private val data = ctx.dataStore("withdrawn_bids")
    private val KEY = stringSetPreferencesKey("ids")

    val ids: Flow<Set<String>> = data.data.map { it[KEY] ?: emptySet() }

    suspend fun add(bidId: Uuid) {
        data.edit { prefs -> prefs[KEY] = (prefs[KEY] ?: emptySet()) + bidId.toString() }
    }

    suspend fun remove(bidId: Uuid) {
        data.edit { prefs -> prefs[KEY] = (prefs[KEY] ?: emptySet()) - bidId.toString() }
    }
}
```

When `withdrawBid` succeeds, add the id; when the refresh comes back and the
bid is gone from the server, remove the local entry.

## Notifications integration

Push types relevant to this screen:
- `bid_accepted` → highlights the accepted card, opens detail
- `offer_rejected` → marks the bid as rejected

See [11-notifications.md](11-notifications.md) for the routing table.
