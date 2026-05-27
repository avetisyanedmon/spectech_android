# 05 — Equipment & Deposit

## Equipment

iOS reference: lines 610–688 of `DomainModels.swift`.

```kotlin
@Serializable
data class Equipment(
    @Contextual val id: Uuid,
    val name: String,
    val category: EquipmentCategory = EquipmentCategory.DUMP_TRUCK,  // lenient fallback
    val characteristics: String = "",
    val additionalEquipment: String? = null,
    val photos: List<String> = emptyList(),
    @Contextual val ownerId: Uuid,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant,
    val depositStatus: DepositStatus? = null,
    @Contextual val depositId: Uuid? = null,
) {
    fun withDeposit(status: DepositStatus?, id: Uuid?): Equipment =
        copy(depositStatus = status, depositId = id)
}
```

### Lenient category decoding

iOS falls back to `dumpTruck` if the category is unknown — this prevents one
stale row from breaking the entire garage list decode. Same intent on Android:
use a try/catch in a custom serializer or default to `DUMP_TRUCK` when an
exception is thrown. The simplest approach is a top-level wrapper:

```kotlin
object LenientEquipmentSerializer : KSerializer<Equipment> {
    private val real = Equipment.serializer()
    override val descriptor = real.descriptor
    override fun serialize(encoder: Encoder, value: Equipment) = real.serialize(encoder, value)
    override fun deserialize(decoder: Decoder): Equipment = try {
        real.deserialize(decoder)
    } catch (_: Exception) {
        // Read raw JSON, patch category, retry — only worth doing if you see real failures.
        throw SerializationException("Equipment row failed to decode")
    }
}
```

Pragmatic alternative: keep category nullable in transport DTO, map after.

## CreateEquipmentRequest

```kotlin
@Serializable
data class CreateEquipmentRequest(
    val name: String,
    val category: EquipmentCategory,  // snake_case wire value
    val characteristics: String,
    val additionalEquipment: String,
    val photos: List<String>,
)
```

The `characteristics` field is a single string like:
`"VIN: ABC123 | Year: 2018 | Description: Lorem ipsum"`. See iOS
`AddEquipmentViewModel.submit()` lines 89-97 for the exact format.

## UpdateEquipmentRequest

All optional — `PATCH /equipment/:id` accepts partial updates.

```kotlin
@Serializable
data class UpdateEquipmentRequest(
    val name: String? = null,
    val characteristics: String? = null,
    val additionalEquipment: String? = null,
    val photos: List<String>? = null,
)
```

## Deposit

The performance security deposit a contractor must pay before being allowed
to bid. Stored on the backend and surfaced via the equipment list.

```kotlin
@Serializable
data class Deposit(
    @Contextual val id: Uuid,
    @Contextual val equipmentId: Uuid,
    val contractorId: String,
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
    val currency: String,
    val status: DepositStatus,
    val confirmationUrl: String? = null,        // YooKassa-hosted payment page
    val paymentMethodId: String? = null,
    @Contextual val paidAt: Instant? = null,
    @Contextual val refundedAt: Instant? = null,
    @Contextual val forfeitedAt: Instant? = null,
    val forfeitedToUserId: String? = null,
    val failureReason: String? = null,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant,
)
```

## CreateDepositRequest / RefundDepositRequest

```kotlin
@Serializable
data class CreateDepositRequest(
    @Contextual val equipmentId: Uuid,
    val returnUrl: String? = null,
    val savePaymentMethod: Boolean = false,
)

@Serializable
data class RefundDepositRequest(val reason: String? = null)
```

## DepositForEquipmentPayload

Response of `GET /payments/deposits/equipment/:id`:

```kotlin
@Serializable
data class DepositForEquipmentPayload(
    @Contextual val equipmentId: Uuid,
    val deposit: Deposit? = null,
    val isPaid: Boolean,
)
```

## Lifecycle summary

```
contractor adds equipment (no deposit)
        │
        ▼
contractor opens Equipment detail → "Performance security" button
        │
        ▼
DepositService.createDeposit(equipmentId)
        ├─► returns Deposit with status = pending + confirmationUrl
        ▼
Chrome Custom Tab opens confirmationUrl (YooKassa)
        ▼
user pays
        ▼
(option A) YooKassa webhook hits backend → backend flips deposit to paid
(option B) user dismisses tab → DepositService.syncDeposit(id) polls backend
        ▼
EquipmentChanged event → Garage and Bid sheet refresh
        ▼
contractor can now submit bids using that equipment
```

The "DEPOSIT_REQUIRED" / HTTP 402 error from `POST /orders/:id/bids` is the
backend's way of saying "deposit not paid". iOS translates this into a
user-friendly message in `BidSheetViewModel.submit()`. Replicate exactly:

```kotlin
if (e is ApiError && (e.code == "DEPOSIT_REQUIRED" || e.statusCode == 402)) {
    error = ApiError(message = ctx.getString(R.string.bid_deposit_required))
    return
}
```
