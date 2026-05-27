# 06 — News & Notification models

## NewsItem

iOS reference: `SpecTechIOS/Scene/Tabs/News/Model/NewsItem.swift`.

```kotlin
@Serializable
data class NewsItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    @Contextual val createdAt: Instant,
)
```

`videoUrl` is optional — when present, render a clickable thumbnail that opens
the URL in a Chrome Custom Tab (or use ExoPlayer if you want inline playback;
iOS doesn't ship inline playback so neither needs to Android initially).

## AppNotification

In-app inbox entry. iOS reference: lines 3–76 of
`SpecTechIOS/Features/Notifications/NotificationStore.swift`.

```kotlin
@Serializable
data class AppNotification(
    @Contextual val id: Uuid = Uuid.random(),
    val notificationId: String? = null,
    val title: String,
    val body: String,
    val type: String? = null,
    val orderId: String? = null,
    val offerId: String? = null,
    val bidId: String? = null,
    @Contextual val receivedAt: Instant = Clock.System.now(),
    var isRead: Boolean = false,           // mutable so markRead can update in-place
) {
    val dedupeKey: String?
        get() {
            if (!notificationId.isNullOrEmpty()) return notificationId
            val parts = listOfNotNull(type, orderId, offerId ?: bidId).filter { it.isNotEmpty() }
            return parts.takeIf { it.isNotEmpty() }?.joinToString(":")
        }

    companion object {
        /** Build from an FCM data payload. */
        fun fromFcmData(data: Map<String, String>, notification: RemoteMessage.Notification?): AppNotification? {
            val title = notification?.title ?: data["title"] ?: return null
            val body = notification?.body ?: data["body"] ?: return null
            return AppNotification(
                title = title,
                body = body,
                notificationId = data["notificationId"],
                type = data["type"],
                orderId = data["orderId"],
                offerId = data["offerId"],
                bidId = data["bidId"] ?: data["offerId"],
            )
        }
    }
}
```

### iOS aps payload vs FCM payload

iOS parses `userInfo["aps"]` for title/body; Android's FCM message exposes
the same data through `RemoteMessage.notification` (display title/body) and
`RemoteMessage.data` (custom fields). The backend has to send custom fields
in the **`data`** part of the FCM payload (not `notification`) so we can read
them regardless of foreground/background state.

The backend currently posts `platform: "ios"` for APNs. The Android registration
call (`POST notifications/register`) must send `platform: "android"` — the
backend already has a switch for routing per-platform.

## NotificationNavigationRequest

Used by the in-app navigation engine to push from a tapped notification.

```kotlin
data class NotificationNavigationRequest(
    val id: String = Uuid.random().toString(),
    val type: String?,
    val orderId: String,
    val offerId: String?,
)
```

iOS exposes this via `NotificationStore.navigationRequest`. The Android
equivalent is a `StateFlow<NotificationNavigationRequest?>` on the same store.

## Notification persistence

iOS uses `UserDefaults` to keep the last 100 notifications. Android uses
Preferences DataStore with a single JSON key:

```kotlin
private val Context.notificationStore by preferencesDataStore("notifications")
private val KEY = stringPreferencesKey("app_notifications")

class NotificationStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()
    val unreadCount: StateFlow<Int> = _notifications
        .map { it.count { n -> !n.isRead } }
        .stateIn(scope, SharingStarted.WhileSubscribed(), 0)

    private val _navigationRequest = MutableStateFlow<NotificationNavigationRequest?>(null)
    val navigationRequest: StateFlow<NotificationNavigationRequest?> = _navigationRequest.asStateFlow()

    private val maxStored = 100
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            ctx.notificationStore.data.first().let { prefs ->
                _notifications.value = prefs[KEY]
                    ?.let { runCatching { Json.decodeFromString<List<AppNotification>>(it) }.getOrDefault(emptyList()) }
                    ?: emptyList()
            }
        }
    }

    fun add(n: AppNotification) {
        var next = n
        val current = _notifications.value
        val dedupeKey = n.dedupeKey
        val deduped = if (dedupeKey != null) {
            val existing = current.firstOrNull { it.dedupeKey == dedupeKey }
            if (existing != null) next = next.copy(isRead = existing.isRead)
            current.filterNot { it.dedupeKey == dedupeKey }
        } else current
        _notifications.value = (listOf(next) + deduped).take(maxStored)
        save()
    }

    fun markRead(id: Uuid) {
        _notifications.update { list -> list.map { if (it.id == id) it.copy(isRead = true) else it } }
        save()
    }

    fun markAllRead() {
        _notifications.update { list -> list.map { it.copy(isRead = true) } }
        save()
    }

    fun clear() {
        _notifications.value = emptyList()
        save()
    }

    fun requestNavigation(req: NotificationNavigationRequest) {
        if (req.orderId.isBlank()) return
        _navigationRequest.value = req
    }

    fun finishNavigationRequest(req: NotificationNavigationRequest) {
        if (_navigationRequest.value?.id == req.id) _navigationRequest.value = null
    }

    private fun save() {
        scope.launch {
            ctx.notificationStore.edit { it[KEY] = Json.encodeToString(_notifications.value) }
        }
    }
}
```

Behavior matches the iOS `NotificationStore` exactly: dedup by either
`notificationId` or `type:orderId:offerId|bidId`, keep last 100, persist
across launches.
