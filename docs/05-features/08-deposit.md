# 08 — Performance Security Deposit

iOS sources:
- `SpecTechIOS/Scene/Tabs/Garage/Deposit/DepositService.swift`
- `SpecTechIOS/Scene/Tabs/Garage/Deposit/DepositInfoSheet.swift`
- `SpecTechIOS/Scene/Tabs/Garage/Deposit/DepositSafariView.swift`

A small but critical sub-feature: contractors must pay a refundable
performance deposit per equipment unit before being allowed to submit bids
with that unit. Payments go through **YooKassa** (`yookassa.ru`) via a
hosted confirmation page.

## State diagram (DepositStatus)

```
created → pending  ──[user pays]──► paid
             │                        │
             │                        └─[user removes equipment]─► refund_pending → refunded
             ▼
        failed (timeout, user cancelled)
```

Statuses (`DepositStatus`):
- `pending` — created on backend, awaiting payment
- `paid` — payment confirmed by YooKassa webhook
- `failed` — payment did not complete
- `refund_pending` — refund initiated, awaiting YooKassa confirmation
- `refunded` — refund complete
- `forfeited` — deposit kept by platform (e.g. dispute resolved against contractor)

`isActiveAndPaid = (status == PAID)` — gates bid submission.
`blocksNewDeposit = (status in {PENDING, PAID, REFUND_PENDING})` — prevents
creating a second deposit while one is in flight.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | `/payments/deposits` | Create a new deposit, returns `confirmationUrl` |
| GET | `/payments/deposits/equipment/{id}` | Get active deposit for an equipment unit (or null) |
| POST | `/payments/deposits/{id}/sync` | Re-query YooKassa via backend, flip status if changed |
| POST | `/payments/deposits/{id}/refund` | Initiate refund |
| GET | `/payments/deposits` | List all deposits for current contractor |

## ViewModel

```kotlin
@HiltViewModel(assistedFactory = DepositInfoViewModel.Factory::class)
class DepositInfoViewModel @AssistedInject constructor(
    @Assisted val equipment: Equipment,
    private val depositRepo: DepositRepository,
    private val events: AppEventBus,
) : ViewModel() {

    @AssistedFactory interface Factory { fun create(eq: Equipment): DepositInfoViewModel }

    var existingDeposit by mutableStateOf<Deposit?>(null)
    var pendingConfirmationUrl by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(true)
    var isPayInFlight by mutableStateOf(false)
    var isSyncInFlight by mutableStateOf(false)
    var error by mutableStateOf<ApiError?>(null)

    val hasPaidDeposit: Boolean get() = existingDeposit?.status == DepositStatus.PAID

    fun load() = viewModelScope.launch {
        isLoading = true
        try {
            existingDeposit = depositRepo.depositForEquipment(equipment.id).deposit
        } catch (e: CancellationException) { throw e }
        catch (e: ApiError) { error = e }
        catch (e: Exception) { error = ApiError.from(e) }
        finally { isLoading = false }
    }

    fun startPayment() = viewModelScope.launch {
        if (isPayInFlight) return@launch
        isPayInFlight = true
        try {
            val existing = existingDeposit
            if (existing?.status == DepositStatus.PENDING && existing.confirmationUrl != null) {
                pendingConfirmationUrl = existing.confirmationUrl
                return@launch
            }
            val deposit = depositRepo.createDeposit(equipment.id)
            existingDeposit = deposit
            pendingConfirmationUrl = deposit.confirmationUrl
                ?: run { error = ApiError(message = "Could not get payment URL."); null }
        } catch (e: ApiError) { error = e }
          catch (e: Exception) { error = ApiError.from(e) }
        finally { isPayInFlight = false }
    }

    fun onPaymentTabClosed() = viewModelScope.launch {
        val id = existingDeposit?.id ?: return@launch
        isSyncInFlight = true
        try {
            existingDeposit = depositRepo.syncDeposit(id)
            events.emit(DomainEvent.EquipmentChanged)
        } catch (e: Exception) {
            // Status sync best-effort
        } finally { isSyncInFlight = false }
    }

    fun refund() = viewModelScope.launch {
        val id = existingDeposit?.id ?: return@launch
        runCatching { depositRepo.refundDeposit(id) }
            .onSuccess { existingDeposit = it; events.emit(DomainEvent.EquipmentChanged) }
            .onFailure { error = ApiError.from(it) }
    }
}
```

## Repository

```kotlin
class DepositRepository @Inject constructor(
    private val api: ApiClient,
    private val events: AppEventBus,
) {
    suspend fun createDeposit(equipmentId: Uuid): Deposit {
        val env = api.send<Deposit>(DepositApi.Create(CreateDepositRequest(equipmentId)))
        return env.data
    }
    suspend fun depositForEquipment(id: Uuid): DepositForEquipmentPayload =
        api.send<DepositForEquipmentPayload>(DepositApi.GetForEquipment(id)).data

    suspend fun syncDeposit(id: Uuid): Deposit {
        val env = api.send<Deposit>(DepositApi.Sync(id))
        events.emit(DomainEvent.EquipmentChanged)
        return env.data
    }

    suspend fun refundDeposit(id: Uuid, reason: String? = null): Deposit {
        val env = api.send<Deposit>(DepositApi.Refund(id, RefundDepositRequest(reason)))
        events.emit(DomainEvent.EquipmentChanged)
        return env.data
    }
}
```

## YooKassa hosted page — Chrome Custom Tabs

iOS opens the URL in `SFSafariViewController`. On Android, use Chrome Custom
Tabs:

```kotlin
fun openPaymentTab(ctx: Context, url: String) {
    val intent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setUrlBarHidingEnabled(false)
        .build()
    intent.launchUrl(ctx, Uri.parse(url))
}
```

There's no callback when the user dismisses a custom tab — we detect it via
`onResume`. iOS does the same thing on the Safari sheet's `onDismiss`:

```kotlin
LaunchedEffect(Unit) {
    lifecycleOwner.lifecycle.eventFlow().filter { it == Lifecycle.Event.ON_RESUME }
        .collect {
            if (vm.pendingConfirmationUrl != null) {
                vm.onPaymentTabClosed()
                vm.pendingConfirmationUrl = null
            }
        }
}
```

The `sync` call flips the deposit to `paid` if YooKassa confirms; otherwise
it stays `pending` and the webhook will flip it shortly afterwards.

## UI flow

```
Equipment detail
  └── [ Performance security ] ─► DepositInfoSheet (modal)
           │
           ├── status: none    → [ Pay deposit ] ─► Chrome Custom Tab
           ├── status: pending → [ Continue payment ] (reuses confirmationUrl)
           ├── status: paid    → "Deposit active" + [ Refund ]
           └── status: refund_pending / refunded / failed / forfeited → text-only
```

When the custom tab returns, run `sync` automatically. The user sees the
status update without further taps.

## Deep link callback (optional)

The deposit `returnUrl` is currently `null` in iOS. If you want a smoother UX
on Android, set it to `spectech://deposit/{equipmentId}` and handle the
deep-link in `MainActivity.onNewIntent`. YooKassa will redirect there on
success.

## Error messages

- 402 / `DEPOSIT_REQUIRED` from `submitBid` → "Pay the performance deposit
  before bidding." (translated in [06-bidding.md](06-bidding.md))
- Network failures during pay → keep status at `pending`, allow retry
- Refund failures → surface inline error, do not change local status

Match the iOS user-facing copy (currently mixed Russian/English) — Russian
copy is in the iOS source files for these messages; reuse those exact
strings in `strings.xml`.
