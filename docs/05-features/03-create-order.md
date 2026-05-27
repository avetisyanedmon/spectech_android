# 03 — Create Order

iOS source: `SpecTechIOS/Features/CreateOrder/CreateOrderView.swift` (~1200 lines).

This is the largest single screen in the app: a multi-step form with 50+
inputs and category-specific options. Port carefully.

## Required (always-visible) fields

| Field | Type | Default |
|---|---|---|
| Equipment category | enum picker | `DUMP_TRUCK` |
| Region | text + region picker | "" |
| City | text + Google Places autocomplete | "" |
| Street | text | "" |
| House number | text | "" |
| Pricing unit | enum picker (9 values) | `PER_HOUR` |
| Selected payment type | enum picker (3 values) | `CASH` |
| Work volume | numeric text | "" |
| Start date | date picker | tomorrow |
| Duration (hours) | numeric stepper | 8 |
| Bidding deadline | date+time picker | start - 1 hour |
| Description | multiline text | "" |

When the user changes start date, the bidding deadline auto-reseeds to
`startDate - 1h` (but clamped to ≥ now). iOS uses `didSet` on `startDate`.
Compose:

```kotlin
var startDate by mutableStateOf(LocalDate.now().plusDays(1))
var biddingDeadline by mutableStateOf(<initial>)
LaunchedEffect(startDate) {
    val auto = (startDate at 12:00).toInstant().minusSeconds(3600)
    biddingDeadline = maxOf(auto, Clock.System.now())
}
```

(Use `kotlinx.datetime` types — `LocalDateTime`, `Instant`.)

## Category-specific (optional) fields

iOS holds these as 25+ `var` properties on `CreateOrderViewModel`. Many are
nullable strings (single-select dropdowns), others numeric. Direct port:

```kotlin
var optBoomLength by mutableStateOf("")
var optLiftCapacity by mutableStateOf("")
var optTonnage by mutableStateOf("")
var optVolumeCubic by mutableStateOf("")
var optDrillingDepth by mutableStateOf("")
var optDrillingDiameter by mutableStateOf("")
var optInertMaterial by mutableStateOf("")

var optSewageType by mutableStateOf<String?>(null)
var optFuelTankerType by mutableStateOf<String?>(null)
var optGraderSize by mutableStateOf<String?>(null)
var optAsphaltPaverSize by mutableStateOf<String?>(null)
var optRoadRollerType by mutableStateOf<String?>(null)
var optSoilCompactorType by mutableStateOf<String?>(null)
var optMiniLoaderBase by mutableStateOf<String?>(null)
var optGarbageTruckType by mutableStateOf<String?>(null)
var optLowbedType by mutableStateOf<String?>(null)
var optDumpTruckAxles by mutableStateOf<String?>(null)
var optFrontLoaderBucket by mutableStateOf<String?>(null)
var optExcavatorBucket by mutableStateOf<String?>(null)
var optTowType by mutableStateOf<String?>(null)
var optBackhoeBucketWidth by mutableStateOf<String?>(null)

val optMiniLoaderAttachments = mutableStateListOf<String>()
val optBackhoeAttachments = mutableStateListOf<String>()
```

Render only the fields relevant to the currently selected `category`. The
`when (category)` block decides which sub-form appears.

## `buildOptionsSummary` — port verbatim

```kotlin
fun buildOptionsSummary(): String? {
    val parts = mutableListOf<String>()
    fun trimmed(s: String) = s.trim().takeIf { it.isNotEmpty() }
    when (category) {
        EquipmentCategory.CONCRETE_PUMP -> trimmed(optBoomLength)?.let { parts += "длина стрелы: $it м" }
        EquipmentCategory.AERIAL_PLATFORM -> trimmed(optBoomLength)?.let { parts += "вылет стрелы: $it м" }
        EquipmentCategory.TRUCK_CRANE, EquipmentCategory.MANIPULATOR -> {
            trimmed(optLiftCapacity)?.let { parts += "грузоподъёмность: $it т" }
            trimmed(optBoomLength)?.let { parts += "длина стрелы: $it м" }
        }
        EquipmentCategory.SEWAGE_VACUUM -> optSewageType?.let { parts += "тип: $it" }
        EquipmentCategory.FUEL_TANKER -> optFuelTankerType?.let { parts += "тип: $it" }
        EquipmentCategory.GRADER -> optGraderSize?.let { parts += "размер: $it" }
        EquipmentCategory.ASPHALT_PAVER -> optAsphaltPaverSize?.let { parts += "размер: $it" }
        EquipmentCategory.ROAD_ROLLER -> {
            optRoadRollerType?.let { parts += "тип: $it" }
            trimmed(optTonnage)?.let { parts += "тоннаж: $it т" }
        }
        EquipmentCategory.SOIL_COMPACTOR -> optSoilCompactorType?.let { parts += "тип: $it" }
        EquipmentCategory.MINI_LOADER -> {
            optMiniLoaderBase?.let { parts += "ходовая: $it" }
            if (optMiniLoaderAttachments.isNotEmpty())
                parts += "доп. оборудование: ${optMiniLoaderAttachments.sorted().joinToString(", ")}"
        }
        EquipmentCategory.MINI_EXCAVATOR -> trimmed(optTonnage)?.let { parts += "тоннаж: $it т" }
        EquipmentCategory.GARBAGE_TRUCK -> optGarbageTruckType?.let { parts += "тип: $it" }
        EquipmentCategory.LOWBED_TRAILER -> optLowbedType?.let { parts += "тип: $it" }
        EquipmentCategory.DUMP_TRUCK -> {
            optDumpTruckAxles?.let { parts += "конфигурация: $it" }
            trimmed(optInertMaterial)?.let { parts += "инертный материал: $it" }
        }
        EquipmentCategory.FRONT_LOADER -> optFrontLoaderBucket?.let { parts += "объём ковша: $it м³" }
        EquipmentCategory.TOW_TRUCK -> optTowType?.let { parts += "тип: $it" }
        EquipmentCategory.EXCAVATOR_CRAWLER, EquipmentCategory.EXCAVATOR_WHEELED ->
            optExcavatorBucket?.let { parts += "объём ковша: $it м³" }
        EquipmentCategory.AUGER_DRILL -> {
            trimmed(optDrillingDepth)?.let { parts += "глубина бурения: $it м" }
            trimmed(optDrillingDiameter)?.let { parts += "диаметр: $it мм" }
        }
        EquipmentCategory.BACKHOE_LOADER -> {
            if (optBackhoeAttachments.isNotEmpty())
                parts += "доп. оборудование: ${optBackhoeAttachments.sorted().joinToString(", ")}"
            optBackhoeBucketWidth?.let { parts += "ширина ковша: $it см" }
        }
        EquipmentCategory.BITUMEN_SPRAYER -> trimmed(optVolumeCubic)?.let { parts += "объём: $it м³" }
        EquipmentCategory.FORKLIFT -> trimmed(optLiftCapacity)?.let { parts += "грузоподъёмность: $it т" }
        EquipmentCategory.CONCRETE_MIXER, EquipmentCategory.BULLDOZER,
        EquipmentCategory.GRAB_LOADER, EquipmentCategory.ROAD_MAINTENANCE_VEHICLE -> Unit
    }
    if (parts.isEmpty()) return null
    return "Параметры техники: " + parts.joinToString("; ") + "."
}
```

The summary is **prepended** to the user's free-form description, separated
by a blank line. The combined string becomes `description` in the request.

## Submit

```kotlin
fun submit() = viewModelScope.launch {
    isSubmitting = true
    runCatching {
        val summary = buildOptionsSummary()
        val combined = listOfNotNull(summary, description.trim().takeIf { it.isNotEmpty() })
            .joinToString("\n\n")

        val request = CreateOrderRequest(
            equipmentCategory = category.backendCreateValue,
            city = city,
            street = street,
            houseNumber = houseNumber,
            address = listOf(region, street, houseNumber).filter { it.isNotEmpty() }.joinToString(", "),
            paymentTypes = listOf(selectedPaymentType.backendCreateValue),
            pricingUnit = pricingUnit.backendCreateValue,
            workVolume = workVolume.toDoubleOrNull() ?: 0.0,
            startDate = startDate.toString(),  // "yyyy-MM-dd"
            startTime = startTime.toString(),  // "HH:mm:ss"
            startDateTime = startDateTime.toString(),
            adDuration = (biddingDeadline.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds())
                .let { (it / 1000).toInt() }
                .coerceAtLeast(60),
            durationHours = durationHours,
            expiryDateTime = biddingDeadline.toString(),
            description = combined.ifEmpty { null },
        )

        ordersRepo.createOrder(request)
    }.onSuccess {
        success = true
        error = null
    }.onFailure { e ->
        error = ApiError.from(e)
    }
    isSubmitting = false
}
```

After success, dismiss the sheet — the orders-changed event will refresh
the My Orders tab.

## Republish (prefill)

The user can delete an order and "Republish" it — opens the CreateOrder
sheet with all fields seeded. iOS `CreateOrderPrefill` parses the order's
fields out. Port to `CreateOrderViewModel.applyPrefill(order: Order)` and
call it in `init`.

The prefill JSON is encoded as a route argument (Navigation Compose accepts
@Serializable types directly):

```kotlin
@Serializable data class CreateOrderRoute(val prefillOrderId: String? = null)
```

Look up the prefill order from the orders repo by id when entering the screen.

## UI structure (Compose)

Use a `LazyColumn` with sections separated by `Divider`. Each section is a
labeled card containing related fields. The first section is "Equipment
type" (category picker), the second is "Address", the third is "Pricing &
payment", the fourth is "Schedule", the fifth is the dynamic
category-specific section, and the last is "Description".

A persistent bottom bar shows the submit button:

```kotlin
Scaffold(
    bottomBar = {
        Surface(tonalElevation = 4.dp) {
            Button(
                onClick = { viewModel.submit() },
                enabled = viewModel.isFormValid && !viewModel.isSubmitting,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                if (viewModel.isSubmitting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                else Text(stringResource(R.string.create_order_submit))
            }
        }
    }
) { padding ->
    LazyColumn(Modifier.padding(padding)) {
        items(sections) { section -> CreateOrderSection(section, viewModel) }
    }
}
```

## Validation

iOS validates inline via `isFormValid` computed property. Mirror:

```kotlin
val isFormValid: Boolean
    get() = region.isNotBlank() && city.isNotBlank() && street.isNotBlank() &&
            houseNumber.isNotBlank() && workVolume.toDoubleOrNull() != null &&
            workVolume.toDouble() > 0
```

Stricter validation can happen on submit; show inline error chips per field
on first invalid attempt.
