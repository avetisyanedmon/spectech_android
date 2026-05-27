# 06 — Bidding

iOS source: `SpecTechIOS/Features/Bidding/BidSheetView.swift`.

A modal sheet presented from `OrderDetailView` when a contractor taps
"Submit a bid".

## Inputs

| Field | Type | Required |
|---|---|---|
| Equipment | dropdown filtered by order's category | yes |
| Price | numeric | yes |
| Delivery price | numeric | yes |
| Payment type | one of the order's `paymentTypes` | yes |
| Comment | multiline text | no |

## ViewModel

```kotlin
@HiltViewModel(assistedFactory = BidSheetViewModel.Factory::class)
class BidSheetViewModel @AssistedInject constructor(
    @Assisted val order: Order,
    private val ordersRepo: OrdersRepository,
    private val equipmentRepo: EquipmentRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    @AssistedFactory interface Factory { fun create(order: Order): BidSheetViewModel }

    var availableEquipment by mutableStateOf<List<Equipment>>(emptyList()); private set
    var selectedEquipment by mutableStateOf<Equipment?>(null)
    var price by mutableStateOf("")
    var deliveryPrice by mutableStateOf("")
    var paymentType by mutableStateOf<PaymentType?>(null)
    var comment by mutableStateOf("")
    var error by mutableStateOf<ApiError?>(null)
    var isSubmitting by mutableStateOf(false); private set
    var successMessage by mutableStateOf<String?>(null); private set
    var showingAddEquipment by mutableStateOf(false)
    var submittedBid by mutableStateOf<Bid?>(null); private set

    val canSubmit: Boolean
        get() = selectedEquipment != null &&
                price.toBigDecimalOrNull() != null &&
                deliveryPrice.toBigDecimalOrNull() != null &&
                paymentType != null &&
                !isSubmitting

    fun load() = viewModelScope.launch {
        runCatching {
            val all = equipmentRepo.fetchEquipment()
            val cat = order.equipmentCategory
            val filtered = if (cat != null) all.filter { it.category == cat } else all
            availableEquipment = filtered
            selectedEquipment = filtered.firstOrNull()
            paymentType = order.paymentTypes.firstOrNull()
        }.onFailure { e -> error = ApiError.from(e) }
    }

    fun reloadEquipmentAfterAdd() = viewModelScope.launch {
        val all = equipmentRepo.fetchEquipment()
        val cat = order.equipmentCategory
        val filtered = if (cat != null) all.filter { it.category == cat } else all
        availableEquipment = filtered
        if (selectedEquipment == null) selectedEquipment = filtered.lastOrNull() ?: filtered.firstOrNull()
    }

    fun submit(ctx: Context) = viewModelScope.launch {
        val user = sessionStore.currentUser ?: run { error = ApiError.MissingSession; return@launch }
        if (order.creatorId?.lowercase() == user.id.toString().lowercase()) {
            error = ApiError(message = ctx.getString(R.string.bid_own_order_error))
            return@launch
        }
        val eq = selectedEquipment ?: return@launch
        val priceDec = price.toBigDecimalOrNull() ?: return@launch
        val payment = paymentType?.takeIf { it in order.paymentTypes }
            ?: run { error = ApiError(message = ctx.getString(R.string.bid_payment_required)); return@launch }

        isSubmitting = true
        try {
            val req = CreateBidRequest(
                price = priceDec,
                deliveryPrice = deliveryPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                paymentType = payment.backendCreateValue,
                comment = comment,
                equipmentId = eq.id,
                equipmentName = eq.name,
                equipmentCategory = eq.category.backendCreateValue,
                equipmentPhotos = eq.photos,
                equipmentCharacteristics = eq.characteristics,
                equipmentAdditionalInfo = eq.additionalEquipment.orEmpty(),
                contractorPhone = user.phone,
                contractorName = user.name.orEmpty(),
            )
            val bid = ordersRepo.submitBid(order.id, req)
            submittedBid = bid
            successMessage = ctx.getString(R.string.bid_submit_success)
            error = null
        } catch (e: ApiError) {
            // Deposit required → 402 / DEPOSIT_REQUIRED
            error = if (e.code == "DEPOSIT_REQUIRED" || e.statusCode == 402) {
                ApiError(message = ctx.getString(R.string.bid_deposit_required))
            } else e
        } finally {
            isSubmitting = false
        }
    }
}
```

## Screen

```kotlin
@Composable
fun BidSheet(
    order: Order,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    val vm: BidSheetViewModel = hiltViewModel(
        creationCallback = { factory: BidSheetViewModel.Factory -> factory.create(order) }
    )
    LaunchedEffect(Unit) { vm.load() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (vm.submittedBid != null) {
            BidSubmittedSuccessView(vm.successMessage!!, onDismiss = onDismiss)
        } else {
            BidForm(vm = vm)
        }
    }

    if (vm.showingAddEquipment) {
        AddEquipmentSheet(
            initialCategory = order.equipmentCategory,
            onAdded = {
                vm.showingAddEquipment = false
                vm.reloadEquipmentAfterAdd()
            },
            onDismiss = { vm.showingAddEquipment = false },
        )
    }
}
```

`BidForm` is a standard column with the fields described above. The
equipment selector is a `DropdownMenu` listing equipment with their photo
thumbnail and name; below the menu, render an "Add equipment" link that
sets `vm.showingAddEquipment = true`.

## Empty garage path

If the contractor has no equipment matching the order's category, show:

```
You don't have any equipment of category "X" yet.

[ Add equipment ]
```

Tapping opens the same in-line Add Equipment sheet. After successful add,
the bid sheet's equipment list reloads and pre-selects the new item.

## Deposit-required UX

When the backend returns 402 / `DEPOSIT_REQUIRED`, surface a clear message
plus a CTA to the deposit flow:

```
This equipment requires a paid performance security deposit before you can submit a bid.

[ Pay deposit ]
```

Tapping "Pay deposit" closes the bid sheet and opens the equipment detail
screen with the deposit info sheet open. iOS handles this manually — the
Android port can do the same or use a side-event channel to coordinate.
