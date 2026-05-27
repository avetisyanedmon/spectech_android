# 07 — Garage

iOS sources:
- `SpecTechIOS/Scene/Tabs/Garage/GarageListView.swift` + `GarageListViewModel.swift`
- `SpecTechIOS/Scene/Tabs/Garage/Equipment/EquipmentDetailView.swift` + `EquipmentCardView.swift`
- `SpecTechIOS/Scene/Tabs/Garage/AddEquipment/AddEquipmentView.swift` + `AddEquipmentViewModel.swift`
- `SpecTechIOS/Scene/Tabs/Garage/EditEquipment/EditEquipmentView.swift` + `EditEquipmentViewModel.swift`

The contractor's inventory of equipment. CRUD with photo uploads.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/equipment` | List all equipment for the current contractor |
| POST | `/equipment` | Create equipment |
| PATCH | `/equipment/{id}` | Update equipment |
| DELETE | `/equipment/{id}` | Delete equipment |
| POST | `/equipment/photos/upload` | Multipart photo upload, returns hosted URL |

## GarageListViewModel

```kotlin
@HiltViewModel
class GarageListViewModel @Inject constructor(
    private val equipmentRepo: EquipmentRepository,
    private val events: AppEventBus,
) : ViewModel() {
    private val _state = MutableStateFlow<RemoteState<List<Equipment>>>(RemoteState.Idle)
    val state: StateFlow<RemoteState<List<Equipment>>> = _state.asStateFlow()
    var showingAddEquipment by mutableStateOf(false)

    init {
        viewModelScope.launch {
            events.events.filterIsInstance<DomainEvent.EquipmentChanged>().collect {
                load(forceRefresh = true)
            }
        }
    }

    fun load(forceRefresh: Boolean = false) = viewModelScope.launch {
        if (!forceRefresh) _state.value = RemoteState.Loading
        try {
            val equipment = equipmentRepo.fetchEquipment()
            _state.value = if (equipment.isEmpty()) RemoteState.Empty(R.string.garage_empty)
                            else RemoteState.Loaded(equipment)
        } catch (e: CancellationException) { throw e }
        catch (e: ApiError) { _state.value = RemoteState.Failed(e) }
        catch (e: Exception) { _state.value = RemoteState.Failed(ApiError.from(e)) }
    }
}
```

## Equipment list UI

`LazyColumn` of `EquipmentCard` rows. Each row shows:
- Hero image (first photo) via Coil `AsyncImage`
- Name, category title, characteristics summary
- A deposit-status badge if `equipment.depositStatus != null`:
  - `paid` → green "Deposit paid" badge
  - `pending` → amber "Deposit pending" badge
  - everything else → no badge

Tap → equipment detail.

## Equipment detail

Shows:
- Photo carousel (`HorizontalPager` with page indicators)
- Name + category
- VIN, Year, Description (parsed from `characteristics`)
- Additional equipment (free-form)
- Buttons:
  - **Edit** → opens edit sheet
  - **Performance security** → opens deposit info sheet
  - **Delete** (destructive, with confirmation) → calls
    `equipmentRepo.deleteEquipment(id)`

## Add Equipment sheet

iOS `AddEquipmentViewModel`:
- Up to 4 photos (PhotosPicker → in-memory `UIImage`s)
- Name (required)
- Category (required)
- VIN (required)
- Year of manufacture (required, free-text — keep as string)
- Description (optional)
- Additional equipment (optional)

Submit performs:

1. Concurrent photo upload via `withThrowingTaskGroup` →
   `equipmentRepo.uploadPhoto(image)` per photo, returns hosted URL.
2. Builds `characteristics` string:
   `"VIN: <vin> | Year: <year> | Description: <desc>"` (description omitted if empty).
3. Calls `POST /equipment` with all hosted URLs.
4. Emits `EquipmentChanged`.

### Android port

```kotlin
@HiltViewModel
class AddEquipmentViewModel @Inject constructor(
    private val equipmentRepo: EquipmentRepository,
) : ViewModel() {

    var name by mutableStateOf("")
    var category by mutableStateOf<EquipmentCategory?>(null)
    var vin by mutableStateOf("")
    var yearOfManufacture by mutableStateOf("")
    var description by mutableStateOf("")
    var additionalEquipment by mutableStateOf("")

    val selectedPhotos = mutableStateListOf<Bitmap>()    // capped at 4

    var isSubmitting by mutableStateOf(false); private set
    var error by mutableStateOf<ApiError?>(null)

    val canSubmit: Boolean
        get() = category != null && name.isNotBlank() && vin.isNotBlank() &&
                yearOfManufacture.isNotBlank() && !isSubmitting

    fun submit(onSuccess: () -> Unit) = viewModelScope.launch {
        val cat = category ?: run {
            error = ApiError(message = "Select an equipment type."); return@launch
        }
        isSubmitting = true
        try {
            val urls = coroutineScope {
                selectedPhotos.map { bmp -> async { equipmentRepo.uploadPhoto(bmp) } }.awaitAll()
            }

            val characteristicsParts = buildList {
                add("VIN: ${vin.trim()}")
                add("Year: ${yearOfManufacture.trim()}")
                description.trim().takeIf { it.isNotEmpty() }?.let { add("Description: $it") }
            }
            val characteristics = characteristicsParts.joinToString(" | ")

            val req = CreateEquipmentRequest(
                name = name.trim(),
                category = cat,
                characteristics = characteristics,
                additionalEquipment = additionalEquipment.trim(),
                photos = urls,
            )
            equipmentRepo.createEquipment(req)
            error = null
            onSuccess()
        } catch (e: ApiError) { error = e }
          catch (e: Exception) { error = ApiError.from(e) }
        finally { isSubmitting = false }
    }
}
```

### Photo picker

Use Android Photo Picker:

```kotlin
val photoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4),
) { uris ->
    coroutineScope.launch {
        viewModel.selectedPhotos.clear()
        uris.take(4).forEach { uri ->
            val bmp = withContext(Dispatchers.IO) {
                ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }
            bmp?.let { viewModel.selectedPhotos.add(it) }
        }
    }
}

Button(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
    Text(stringResource(R.string.add_photos))
}
```

### Photo encoding + upload

See [07-infrastructure/03-image-encoding-upload.md](../07-infrastructure/03-image-encoding-upload.md).

## Edit Equipment

`EditEquipmentViewModel` is the same shape minus VIN/year locking (the user
can edit name, characteristics, additional equipment, and replace photos).

Photos: iOS lets the user remove existing photos and add new ones; the
combined list (existing URLs + new uploads) is sent as `photos`.

`PATCH /equipment/{id}` accepts partial updates — the iOS service sends
the full `UpdateEquipmentRequest` with all four fields nullable. Android
matches.

## Delete equipment

Wrap in a confirmation dialog. After delete, the equipment-changed event
will refresh both the garage list (via observer) and any open bid sheets
that hold a cached list.

If the equipment had a `paid` deposit, the deposit must be refunded first
(or surfaces an inline message). The iOS deposit flow's "Refund" button
lives on the equipment detail in the deposit info sheet.
