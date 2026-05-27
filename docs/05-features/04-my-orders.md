# 04 — My Orders

iOS sources:
- `SpecTechIOS/Scene/Tabs/MyOrders/MyOrdersViewModel.swift`
- `SpecTechIOS/Scene/Tabs/MyOrders/MyOrdersView.swift`

The customer's view of orders they themselves created.

## Endpoint

`GET /orders?view=mine` (paged, 50/page, requires auth).

## ViewModel

Identical structure to `MarketplaceViewModel`, with `scope = OrderScope.MINE`,
no filters, and one extra capability: **accept a bid** on the order detail
screen.

```kotlin
@HiltViewModel
class MyOrdersViewModel @Inject constructor(
    private val ordersRepo: OrdersRepository,
    private val events: AppEventBus,
) : ViewModel() {

    private val _state = MutableStateFlow<RemoteState<List<Order>>>(RemoteState.Idle)
    val state: StateFlow<RemoteState<List<Order>>> = _state.asStateFlow()
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

    fun load(forceRefresh: Boolean = false) = viewModelScope.launch { /* identical pattern */ }
    fun loadMoreIfNeeded(currentItem: Order) = viewModelScope.launch { /* identical */ }
    fun reset() { _state.value = RemoteState.Idle }
}
```

## Accept-bid flow

In the order detail screen (used by both Marketplace and My Orders), when
the current user is the order's creator, render an **Accept** button per
incoming bid:

```kotlin
@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val ordersRepo: OrdersRepository,
    private val sessionStore: SessionStore,
    private val events: AppEventBus,
) : ViewModel() {
    var acceptedContact by mutableStateOf<ContractorContact?>(null)
    var acceptError by mutableStateOf<ApiError?>(null)
    var isAccepting by mutableStateOf(false)

    fun accept(orderId: Uuid, bid: Bid) = viewModelScope.launch {
        isAccepting = true
        runCatching {
            ordersRepo.acceptBid(orderId, bid.id, bid.contractorId)
        }.onSuccess { acceptedContact = it; events.emit(DomainEvent.OrdersChanged) }
         .onFailure { acceptError = ApiError.from(it) }
        isAccepting = false
    }
}
```

The "Accept" action:
1. Sends `POST /orders/{orderId}/bids/{bidId}/accept`.
2. Server fires a push notification to the contractor (`bid_accepted`).
3. Response includes `ContractorContact { phone, name }` — show in a
   "Bid accepted" success card with a "Call" button:

```kotlin
@Composable
fun PhoneActionButton(contact: ContractorContact) {
    val ctx = LocalContext.current
    Button(onClick = {
        ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}")))
    }) { Text(stringResource(R.string.call_contractor)) }
}
```

iOS also exposes a "Copy" action (`PhoneActionButton.swift`). On Android,
add a long-press menu with "Copy phone".

### Fallback for missing phone

iOS's `OrdersService.acceptBid` retries with `fetchContractor(id:)` if the
accept response comes back with no phone. Replicate:

```kotlin
suspend fun acceptBid(orderId: Uuid, bidId: Uuid, contractorId: Uuid?): ContractorContact {
    val env = api.send<ContractorContact>(OrdersApi.AcceptBid(orderId, bidId))
    events.emit(DomainEvent.OrdersChanged)
    val primary = env.data
    if (!primary.phone.isNullOrBlank()) return primary
    if (contractorId == null) return primary
    return runCatching { fetchContractor(contractorId) }.getOrDefault(primary)
}
```

## Delete order

Customer-only action on their own draft/open order. Calls
`DELETE /orders/{id}` and emits `OrdersChanged`.

```kotlin
fun deleteOrder(id: Uuid) = viewModelScope.launch {
    runCatching { ordersRepo.deleteOrder(id) }
        .onFailure { deleteError = ApiError.from(it) }
}
```

## Republish

iOS callback `onRepublish: (Order) -> Void` opens the CreateOrder sheet with
a prefill seeded from the deleted order. On Android, the My Orders detail
screen exposes a "Republish" button:

```kotlin
Button(onClick = { onRepublish(order) }) { Text(stringResource(R.string.republish)) }
```

…which navigates to `CreateOrderRoute(prefillOrderId = order.id.toString())`.
The CreateOrder VM looks up the order by id and applies the prefill.

## Screen states

| State | UI |
|---|---|
| Not authenticated | `SignInPromptView` with "Sign In" button |
| Loading | spinner |
| Loaded (empty) | "You haven't created any orders yet" + "Create Order" button |
| Loaded | List of order cards with status badges |
| Failed | `ErrorStateView` with retry button |

## Pagination

Same pattern as Marketplace: `onLastItemVisible(order) → loadMoreIfNeeded(order)`.
