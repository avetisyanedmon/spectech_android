# 02 — Enums

All enums are wire-aligned. Where the backend accepts either a canonical
snake-case code (`"per_hour"`) or a Russian display string (`"за час"`), the
Android deserializer mirrors iOS's lenient behavior.

Reference: `SpecTechIOS/Shared/Models/DomainModels.swift`

## UserRole

```kotlin
@Serializable
enum class UserRole(val wire: String) {
    @SerialName("customer") CUSTOMER("customer"),
    @SerialName("contractor") CONTRACTOR("contractor"),
    @SerialName("admin") ADMIN("admin");
    fun titleRes(): Int = when (this) {
        CUSTOMER -> R.string.role_customer
        CONTRACTOR -> R.string.role_contractor
        ADMIN -> R.string.role_admin
    }
}
```

## EquipmentCategory (27 cases)

The backend stores categories as Russian strings when creating an order
(e.g. `"Самосвал"`) but supports new snake_case codes everywhere. The iOS
decoder accepts both forms plus legacy aliases (`"excavator"`, `"loader"`,
`"crane"`, `"compactor"`). Replicate this:

```kotlin
@Serializable(with = EquipmentCategorySerializer::class)
enum class EquipmentCategory(
    val wire: String,             // snake_case sent to the backend
    val backendCreateValue: String, // Russian display string for POST /orders body
    @StringRes val titleRes: Int,
) {
    CONCRETE_PUMP("concrete_pump", "Автобетононасос", R.string.cat_concrete_pump),
    AERIAL_PLATFORM("aerial_platform", "Автовышка", R.string.cat_aerial_platform),
    TRUCK_CRANE("truck_crane", "Автокран", R.string.cat_truck_crane),
    SEWAGE_VACUUM("sewage_vacuum", "Ассенизатор-илосос", R.string.cat_sewage_vacuum),
    FUEL_TANKER("fuel_tanker", "Бензовоз / Автоцистерна", R.string.cat_fuel_tanker),
    CONCRETE_MIXER("concrete_mixer", "Бетоновоз", R.string.cat_concrete_mixer),
    BULLDOZER("bulldozer", "Бульдозер", R.string.cat_bulldozer),
    GRADER("grader", "Грейдер", R.string.cat_grader),
    GRAB_LOADER("grab_loader", "Грейферный погрузчик", R.string.cat_grab_loader),
    ROAD_ROLLER("road_roller", "Дорожный каток", R.string.cat_road_roller),
    SOIL_COMPACTOR("soil_compactor", "Грунтовый каток", R.string.cat_soil_compactor),
    ASPHALT_PAVER("asphalt_paver", "Асфальтоукладчик", R.string.cat_asphalt_paver),
    MANIPULATOR("manipulator", "Манипулятор", R.string.cat_manipulator),
    MINI_LOADER("mini_loader", "Мини-погрузчик", R.string.cat_mini_loader),
    MINI_EXCAVATOR("mini_excavator", "Мини-экскаватор", R.string.cat_mini_excavator),
    GARBAGE_TRUCK("garbage_truck", "Мусоровоз / Бункеровоз / Ломовоз", R.string.cat_garbage_truck),
    LOWBED_TRAILER("lowbed_trailer", "Трал / Низкорамная платформа", R.string.cat_lowbed_trailer),
    DUMP_TRUCK("dump_truck", "Самосвал", R.string.cat_dump_truck),
    FRONT_LOADER("front_loader", "Фронтальный погрузчик", R.string.cat_front_loader),
    TOW_TRUCK("tow_truck", "Эвакуатор / Автовоз", R.string.cat_tow_truck),
    EXCAVATOR_CRAWLER("excavator_crawler", "Экскаватор гусеничный", R.string.cat_excavator_crawler),
    EXCAVATOR_WHEELED("excavator_wheeled", "Экскаватор колесный", R.string.cat_excavator_wheeled),
    AUGER_DRILL("auger_drill", "Ямобур", R.string.cat_auger_drill),
    BACKHOE_LOADER("backhoe_loader", "Экскаватор-погрузчик", R.string.cat_backhoe_loader),
    BITUMEN_SPRAYER("bitumen_sprayer", "Гудронатор", R.string.cat_bitumen_sprayer),
    FORKLIFT("forklift", "Вилочный погрузчик", R.string.cat_forklift),
    ROAD_MAINTENANCE_VEHICLE("road_maintenance_vehicle", "КДМ", R.string.cat_road_maintenance_vehicle);

    companion object {
        fun normalized(raw: String): EquipmentCategory? {
            val trimmed = raw.trim()
            val lowered = trimmed.lowercase().replace('-', '_')
            entries.firstOrNull { it.wire == lowered }?.let { return it }
            val ruLower = trimmed.lowercase()
            entries.firstOrNull { it.backendCreateValue.lowercase() == ruLower }?.let { return it }
            return when (lowered) {
                "excavator", "экскаватор" -> EXCAVATOR_CRAWLER
                "loader", "погрузчик"     -> FRONT_LOADER
                "crane", "кран"           -> TRUCK_CRANE
                "compactor", "каток"      -> ROAD_ROLLER
                else -> null
            }
        }
    }
}

object EquipmentCategorySerializer : KSerializer<EquipmentCategory> {
    override val descriptor = PrimitiveSerialDescriptor("EquipmentCategory", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: EquipmentCategory) =
        encoder.encodeString(value.wire)
    override fun deserialize(decoder: Decoder): EquipmentCategory {
        val raw = decoder.decodeString()
        return EquipmentCategory.normalized(raw)
            ?: throw SerializationException("Unknown EquipmentCategory value: $raw")
    }
}
```

For parent decoders that need to tolerate unknown values without failing the
entire payload (used by `Order` and `Bid`), use a nullable-tolerant decoder
(`try…catch` and assign null) inside the parent's custom deserializer. iOS
does this via `try? c.decodeIfPresent(...)`.

## PricingUnit

```kotlin
@Serializable(with = PricingUnitSerializer::class)
enum class PricingUnit(val wire: String, val backendCreateValue: String, @StringRes val titleRes: Int) {
    PER_HOUR("per_hour", "за час", R.string.unit_hour),
    PER_SHIFT("per_shift", "за смену", R.string.unit_shift),
    PER_M3("per_m3", "за м3", R.string.unit_m3),
    PER_TON("per_ton", "за тонну", R.string.unit_ton),
    PER_KM("per_km", "за км", R.string.unit_km),
    PER_TON_KM("per_ton_km", "за т*км", R.string.unit_ton_km),
    PER_M3_KM("per_m3_km", "за м3*км", R.string.unit_m3_km),
    PER_M2("per_m2", "за м2", R.string.unit_m2),
    PER_LINEAR_M("per_linear_m", "за погонный метр", R.string.unit_linear_m);

    companion object {
        fun normalized(raw: String): PricingUnit? = when (raw.trim().lowercase()) {
            "per_hour", "за час" -> PER_HOUR
            "per_shift", "за смену" -> PER_SHIFT
            "per_m3", "за м3", "за м^3" -> PER_M3
            "per_ton", "за тонну" -> PER_TON
            "per_ton_km", "за т*км", "за т-км" -> PER_TON_KM
            "per_m3_km", "за м3*км", "за м3-км" -> PER_M3_KM
            "per_m2", "за м2" -> PER_M2
            "per_linear_m", "за погонный метр" -> PER_LINEAR_M
            "per_km", "за км" -> PER_KM
            else -> null
        }
    }
}
```

## PaymentType

```kotlin
@Serializable(with = PaymentTypeSerializer::class)
enum class PaymentType(val wire: String, val backendCreateValue: String, @StringRes val titleRes: Int) {
    CASH("cash", "наличные", R.string.payment_cash),
    NDS("nds", "с ндс", R.string.payment_nds),
    USN("usn", "усн", R.string.payment_usn);

    companion object {
        fun normalized(raw: String): PaymentType? = when (raw.trim().lowercase()) {
            "cash", "наличные", "наличка" -> CASH
            "nds", "с ндс", "ндс" -> NDS
            "usn", "усн" -> USN
            else -> null
        }
    }
}
```

## OrderScope (URL query value, not stored)

```kotlin
enum class OrderScope(val wire: String, @StringRes val titleRes: Int) {
    ALL("all", R.string.scope_all),
    MINE("mine", R.string.scope_mine),
    MARKETPLACE("marketplace", R.string.scope_marketplace),
    PENDING("pending", R.string.scope_pending),
}
```

## OrderStatus (lenient decoder)

```kotlin
@Serializable(with = OrderStatusSerializer::class)
enum class OrderStatus(val wire: String, @StringRes val titleRes: Int) {
    OPEN("open", R.string.status_open),
    PENDING("pending", R.string.status_pending),
    ACCEPTED("accepted", R.string.status_accepted),
    IN_PROGRESS("in_progress", R.string.status_in_progress),
    COMPLETED("completed", R.string.status_completed),
    CANCELLED("cancelled", R.string.status_cancelled),
    EXPIRED("expired", R.string.status_expired),
    CLOSED("closed", R.string.status_closed);
}

object OrderStatusSerializer : KSerializer<OrderStatus> {
    override val descriptor = PrimitiveSerialDescriptor("OrderStatus", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: OrderStatus) =
        encoder.encodeString(value.wire)
    override fun deserialize(decoder: Decoder): OrderStatus =
        when (decoder.decodeString().trim().lowercase()) {
            "open" -> OrderStatus.OPEN
            "pending" -> OrderStatus.PENDING
            "accepted" -> OrderStatus.ACCEPTED
            "in_progress", "in progress" -> OrderStatus.IN_PROGRESS
            "completed" -> OrderStatus.COMPLETED
            "cancelled", "canceled" -> OrderStatus.CANCELLED
            "expired" -> OrderStatus.EXPIRED
            "closed" -> OrderStatus.CLOSED
            else -> throw SerializationException("Unknown OrderStatus")
        }
}
```

## DepositStatus

```kotlin
@Serializable
enum class DepositStatus(val wire: String) {
    @SerialName("pending") PENDING("pending"),
    @SerialName("paid") PAID("paid"),
    @SerialName("failed") FAILED("failed"),
    @SerialName("refund_pending") REFUND_PENDING("refund_pending"),
    @SerialName("refunded") REFUNDED("refunded"),
    @SerialName("forfeited") FORFEITED("forfeited");

    val isActiveAndPaid: Boolean get() = this == PAID
    val blocksNewDeposit: Boolean get() = this in setOf(PENDING, PAID, REFUND_PENDING)
}
```

## AppLanguage

```kotlin
@Serializable
enum class AppLanguage(val wire: String, val locale: String) {
    @SerialName("english") ENGLISH("english", "en"),
    @SerialName("russian") RUSSIAN("russian", "ru");
}
```

Per-app locale change uses `AppCompatDelegate.setApplicationLocales(...)`
or, on API 33+, the system's per-app language preference.

## EquipmentStatus (UI-only)

iOS has `EquipmentStatus` (`available`, `inUse`, `maintenance`) used purely
for UI display. It's not wire-serialized and the backend doesn't store it.
Port as a plain enum in `core/ui-kit` since it has no domain meaning.
