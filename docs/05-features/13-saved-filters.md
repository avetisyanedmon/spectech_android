# 13 — Saved Filters & Match-Order Notifications

iOS source: `SpecTechIOS/Scene/Tabs/Marketplace/Filters/SavedFilterStore.swift`.

The user can save one `OrderFilters` instance server-side and opt into push
notifications when a **new order matches** that filter. The backend matcher
(`services/savedFilters.service.js`) fans out a push to every enabled
subscription as soon as a customer creates a matching order.

## State

- `savedFilter: OrderFilters?` — the persisted filter
- `notificationsEnabled: Bool` — opt-in toggle
- `isSyncing: Bool` — debounce/spinner state
- `lastSyncError: String?` — surfaces backend errors inline

Persisted locally in DataStore so the UI doesn't flicker on cold start:

| Key | Type |
|---|---|
| `spectech.savedFilter.v1` | JSON-encoded `OrderFilters` |
| `spectech.savedFilter.notificationsEnabled.v1` | Boolean |

Server is the canonical store; local is a mirror for offline UI.

## Endpoints

See [04-networking/04-endpoints.md#saved-filter](../04-networking/04-endpoints.md).

| Method | Path | Purpose |
|---|---|---|
| GET | `/notifications/saved-filter` | Read user's saved filter & toggle (404 if none) |
| PUT | `/notifications/saved-filter` | Upsert filter + enabled state |
| POST | `/notifications/saved-filter/enabled` | Toggle enabled flag without sending filter |
| DELETE | `/notifications/saved-filter` | Clear saved filter |

## Store

```kotlin
@Singleton
class SavedFilterStore @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val api: ApiClient,
) {
    private val data = ctx.dataStore("saved_filter")
    private val FILTER_KEY = stringPreferencesKey("filter_v1")
    private val ENABLED_KEY = booleanPreferencesKey("notifications_enabled_v1")

    private val _savedFilter = MutableStateFlow<OrderFilters?>(null)
    val savedFilter: StateFlow<OrderFilters?> = _savedFilter.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    suspend fun initLocal() {
        val prefs = data.data.first()
        _savedFilter.value = prefs[FILTER_KEY]?.let {
            runCatching { Json.decodeFromString<OrderFilters>(it) }.getOrNull()
        }
        _notificationsEnabled.value = prefs[ENABLED_KEY] ?: false
    }

    suspend fun save(filter: OrderFilters) {
        _savedFilter.value = filter
        persistFilterLocally(filter)
        syncToServer(filter, _notificationsEnabled.value)
    }

    suspend fun setNotificationsEnabled(enabled: Boolean, requestPermission: suspend () -> Boolean) {
        _notificationsEnabled.value = enabled
        data.edit { it[ENABLED_KEY] = enabled }

        if (enabled) {
            val granted = requestPermission()
            if (!granted) {
                _notificationsEnabled.value = false
                data.edit { it[ENABLED_KEY] = false }
                return
            }
            val filter = _savedFilter.value
            if (filter == null) {
                _lastSyncError.value = "Select at least one filter before enabling notifications."
                return
            }
            syncToServer(filter, true)
        } else {
            disableOnServer()
        }
    }

    suspend fun clear() {
        _savedFilter.value = null
        data.edit { it.remove(FILTER_KEY) }
        deleteOnServer()
    }

    suspend fun loadFromServer() {
        try {
            val env = api.send<SavedFilterResponse>(SavedFilterApi.Fetch)
            env.data.filters?.let {
                val merged = it.toOrderFilters()
                _savedFilter.value = merged
                persistFilterLocally(merged)
            }
            env.data.enabled?.let { _notificationsEnabled.value = it }
            _lastSyncError.value = null
        } catch (e: ApiError) {
            if (e.statusCode == 404) {
                _lastSyncError.value = null   // none yet on server
            } else {
                _lastSyncError.value = e.message
            }
        } catch (e: Exception) {
            _lastSyncError.value = e.localizedMessage
        }
    }

    private suspend fun syncToServer(filter: OrderFilters, enabled: Boolean) {
        _isSyncing.value = true
        try {
            val payload = filter.toPayload()
            api.send<SavedFilterResponse>(SavedFilterApi.Upsert(
                UpsertSavedFilterRequest(payload, enabled)
            ))
            _lastSyncError.value = null
        } catch (e: Exception) {
            _lastSyncError.value = e.localizedMessage
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun disableOnServer() {
        _isSyncing.value = true
        try {
            api.send<SavedFilterResponse>(SavedFilterApi.SetEnabled(SetSavedFilterEnabledRequest(false)))
            _lastSyncError.value = null
        } catch (e: ApiError) {
            if (e.statusCode != 404) _lastSyncError.value = e.message
        } catch (e: Exception) {
            _lastSyncError.value = e.localizedMessage
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun deleteOnServer() {
        _isSyncing.value = true
        try {
            api.send<SavedFilterDeletedResponse>(SavedFilterApi.Delete)
            _lastSyncError.value = null
        } catch (e: ApiError) {
            if (e.statusCode != 404) _lastSyncError.value = e.message
        } catch (e: Exception) {
            _lastSyncError.value = e.localizedMessage
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun persistFilterLocally(filter: OrderFilters) {
        data.edit { it[FILTER_KEY] = Json.encodeToString(filter) }
    }
}
```

### `OrderFilters` ↔ `SavedFilterPayload`

```kotlin
@Serializable
data class SavedFilterPayload(
    val categories: List<String>,
    val regions: List<String>,
    val cities: List<String>,
    val pricingUnits: List<String>,
    val paymentTypes: List<String>,
)

fun OrderFilters.toPayload() = SavedFilterPayload(
    categories = categories.map { it.wire }.sorted(),
    regions = regions.sorted(),
    cities = selectedCities.sorted(),
    pricingUnits = pricingUnits.map { it.wire }.sorted(),
    paymentTypes = paymentTypes.map { it.wire }.sorted(),
)

fun SavedFilterPayload.toOrderFilters() = OrderFilters(
    categories = categories.mapNotNull { code -> EquipmentCategory.entries.firstOrNull { it.wire == code } }.toSet(),
    regions = regions.toSet(),
    selectedCities = cities.toSet(),
    pricingUnits = pricingUnits.mapNotNull { code -> PricingUnit.entries.firstOrNull { it.wire == code } }.toSet(),
    paymentTypes = paymentTypes.mapNotNull { code -> PaymentType.entries.firstOrNull { it.wire == code } }.toSet(),
)
```

## UI surface

Two places expose this:

1. **Inside the Marketplace filter sheet** — a section with:
   - "Save these filters" button (disabled if `filters.isEmpty()`)
   - Switch: "Notify me when a new order matches"
   - Inline error message when `lastSyncError != null`
   - "Clear saved filter" link (only when one exists)

2. **Profile menu** — same toggle, exposed for users who already saved a filter
   and want to flip notifications off without opening the marketplace sheet.

## requestPermission lambda

The `setNotificationsEnabled(enabled, requestPermission)` parameter is a
suspend function the caller provides to launch the `POST_NOTIFICATIONS`
permission flow on API 33+. Typical wiring:

```kotlin
val ctx = LocalContext.current
val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* … */ }
val permissionResult = remember { CompletableDeferred<Boolean>() }

suspend fun requestNotificationPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    if (ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        return true
    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    return permissionResult.await()
}
```

(For brevity, this code sketch elides the `permissionResult.complete(...)`
call inside the registerForActivityResult callback — your real
implementation should wire both ends.)
