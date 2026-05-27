# 04 — Order & Bid

## Bid

iOS reference: lines 321–431 of `DomainModels.swift`.

```kotlin
@Serializable
data class Bid(
    @Contextual val id: Uuid,
    @Contextual val userId: Uuid? = null,
    @Contextual val contractorId: Uuid? = null,
    @Serializable(with = BigDecimalSerializer::class) val price: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val deliveryPrice: BigDecimal? = null,
    val paymentType: PaymentType? = null,
    val comment: String? = null,
    @Contextual val equipmentId: Uuid? = null,
    val equipmentName: String? = null,
    val equipmentCategory: EquipmentCategory? = null,
    val equipmentPhotos: List<String>? = null,
    val equipmentCharacteristics: String? = null,
    val equipmentAdditionalInfo: String? = null,
    val isAccepted: Boolean = false,
    @Contextual val submittedAt: Instant? = null,
    @Contextual val createdAt: Instant? = null,
    @Contextual val updatedAt: Instant? = null,
    val contractorPhone: String? = null,
    val contractorName: String? = null,
    val contractor: ContractorInfo? = null,
    val user: ContractorInfo? = null,
    val submittedBy: ContractorInfo? = null,
) {
    val contractorContact: ContractorContact?
        get() {
            val nested = contractor ?: user ?: submittedBy
            val phone = nested?.phone ?: contractorPhone
            val name = nested?.name ?: contractorName
            return if (phone != null) ContractorContact(phone, name) else null
        }
}
```

### Lenient decoding

iOS's `Bid.init(from:)` uses `try?` on `equipmentCategory` so an unknown
category does not fail the whole decode. Achieve this with a custom
serializer that wraps `EquipmentCategorySerializer`:

```kotlin
object NullableEquipmentCategorySerializer : KSerializer<EquipmentCategory?> {
    override val descriptor = EquipmentCategorySerializer.descriptor.nullable
    override fun serialize(encoder: Encoder, value: EquipmentCategory?) {
        if (value == null) encoder.encodeNull() else EquipmentCategorySerializer.serialize(encoder, value)
    }
    override fun deserialize(decoder: Decoder): EquipmentCategory? = try {
        EquipmentCategorySerializer.deserialize(decoder)
    } catch (_: Exception) {
        decoder.decodeNotNullMark(); null
    }
}
```

…and on the field: `@Serializable(with = NullableEquipmentCategorySerializer::class) val equipmentCategory: EquipmentCategory? = null`.

Use the same trick for `Order.equipmentCategory`.

## Order

```kotlin
@Serializable
data class Order(
    @Contextual val id: Uuid,
    @Serializable(with = NullableEquipmentCategorySerializer::class) val equipmentCategory: EquipmentCategory? = null,
    val city: String,
    val address: String? = null,
    val description: String? = null,
    val paymentTypes: List<PaymentType> = emptyList(),
    val pricingUnit: PricingUnit? = null,
    val workVolume: Double? = null,
    @Contextual val startDateTime: Instant? = null,
    val durationHours: Int? = null,
    @Contextual val expiresAt: Instant? = null,
    @Contextual val expiryDateTime: Instant? = null,
    val status: OrderStatus = OrderStatus.OPEN,
    val bidCount: Int = 0,
    val creatorId: String? = null,
    val creatorPhone: String? = null,
    val creatorName: String? = null,
    @Contextual val createdAt: Instant? = null,
    val bids: List<Bid> = emptyList(),
    @Contextual val acceptedBidId: Uuid? = null,
) {
    val effectiveExpiry: Instant? get() = expiresAt ?: expiryDateTime
    val isExpired: Boolean get() = effectiveExpiry?.let { it < Clock.System.now() } == true
    val displayAddress: String get() = address?.takeIf { it.isNotEmpty() } ?: city

    val fullAddress: String get() {
        val a = address?.trim().orEmpty()
        val c = city.trim()
        if (a.isEmpty()) return c
        if (c.isEmpty()) return a
        return "$c, $a"
    }
}
```

### Why duplicate `expiresAt` and `expiryDateTime`

The backend has shipped both field names at different times; iOS keeps both
nullable and picks whichever is present (`effectiveExpiry`). Replicate.

## OrderFilters

```kotlin
@Serializable
data class OrderFilters(
    val categories: Set<EquipmentCategory> = emptySet(),
    val regions: Set<String> = emptySet(),
    val selectedCities: Set<String> = emptySet(),
    val pricingUnits: Set<PricingUnit> = emptySet(),
    val paymentTypes: Set<PaymentType> = emptySet(),
) {
    val isEmpty: Boolean
        get() = categories.isEmpty() && regions.isEmpty() && selectedCities.isEmpty() &&
                pricingUnits.isEmpty() && paymentTypes.isEmpty()

    fun matches(order: Order): Boolean {
        if (categories.isNotEmpty()) {
            val cat = order.equipmentCategory ?: return false
            if (cat !in categories) return false
        }
        if (selectedCities.isNotEmpty() &&
            selectedCities.none { it.lowercase() in order.city.lowercase() }) return false
        if (regions.isNotEmpty()) {
            val haystack = order.address.orEmpty().lowercase()
            if (regions.none { it.lowercase() in haystack }) return false
        }
        if (pricingUnits.isNotEmpty()) {
            val unit = order.pricingUnit ?: return false
            if (unit !in pricingUnits) return false
        }
        if (paymentTypes.isNotEmpty()) {
            if (order.paymentTypes.toSet().intersect(paymentTypes).isEmpty()) return false
        }
        return true
    }
}
```

Note the **bug-compatible parts**:
- If `categories` is non-empty AND the order's category is null → exclude.
  iOS:`if !categories.isEmpty(), order.equipmentCategory == nil { return false }`
- Region match uses substring on the order's `address` field, not a separate
  region column. iOS keeps the comment explaining why.

## CreateOrderRequest

The wire shape for `POST /orders`. iOS sends a flat object with both
camelCase and Russian display values:

```kotlin
@Serializable
data class CreateOrderRequest(
    val equipmentCategory: String,    // Russian display value, e.g. "Самосвал"
    val city: String,
    val street: String,
    val houseNumber: String,
    val address: String,              // "region, street, house" joined
    val paymentTypes: List<String>,   // Russian display values
    val pricingUnit: String,          // Russian display value
    val workVolume: Double,
    val startDate: String,            // ISO yyyy-MM-dd
    val startTime: String,            // ISO HH:mm:ss
    val startDateTime: String,        // ISO-8601 full
    val adDuration: Int,              // bidding window in seconds (iOS computes from biddingDeadline)
    val durationHours: Int,
    val expiryDateTime: String,       // ISO-8601 full
    val description: String? = null,
)
```

The `description` field is the user's description optionally prefixed by the
options summary built from category-specific fields (see iOS
`buildOptionsSummary()` in `CreateOrderView.swift` lines 130-201) — port that
function verbatim to Kotlin.

## CreateBidRequest

```kotlin
@Serializable
data class CreateBidRequest(
    @Serializable(with = BigDecimalSerializer::class) val price: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val deliveryPrice: BigDecimal,
    val paymentType: String,             // Russian display value
    val comment: String,
    @Contextual val equipmentId: Uuid,
    val equipmentName: String,
    val equipmentCategory: String,        // Russian display value
    val equipmentPhotos: List<String>,
    val equipmentCharacteristics: String,
    val equipmentAdditionalInfo: String,
    val contractorPhone: String,
    val contractorName: String,
)
```

## BigDecimal serializer

`Decimal` on iOS, `BigDecimal` on Android. The backend accepts JSON numbers,
not strings. Encode without quoting:

```kotlin
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.DOUBLE)
    override fun serialize(encoder: Encoder, value: BigDecimal) =
        encoder.encodeDouble(value.toDouble())   // backend accepts JSON number
    override fun deserialize(decoder: Decoder): BigDecimal =
        BigDecimal(decoder.decodeDouble())
}
```

If the backend ever switches to string-encoded decimals, swap to
`encodeString(value.toPlainString())` / `BigDecimal(decoder.decodeString())`.
