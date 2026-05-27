# 11 — Notifications

iOS sources:
- `SpecTechIOS/Features/Notifications/NotificationStore.swift`
- `SpecTechIOS/Features/Notifications/PushRegistrationService.swift`
- `SpecTechIOS/Features/Notifications/NotificationsView.swift`
- `SpecTechIOS/App/AppDelegate.swift`
- `SpecTechIOS/Scene/Root/MainTabView.swift` (`handleNotificationNavigation`)

This is the most cross-cutting feature: it involves FCM, secure storage,
persistence, deep linking, and per-tab navigation. Get this right.

## Two pieces

1. **In-app inbox** (UI list of past notifications) — stored locally in
   Preferences DataStore.
2. **Push registration** — register the FCM token with backend on sign-in,
   unregister on sign-out.

## Push types and routing

iOS defines the routing in `MainTabView.notificationTarget(for:)`:

| `type` field in payload | Target tab | Order scope |
|---|---|---|
| `new_bid`, `offer_created` | My Orders | `mine` |
| `bid_accepted`, `offer_accepted`, `offer_rejected` | My Bids | `pending` |
| `matching_order`, `new_matching_order` | Marketplace | `marketplace` |
| (anything else) | Marketplace | `all` |

After routing to the tab, fetch the orders list with that scope, find the
order by `orderId`, and push the order detail onto that tab's nav stack.

## FCM payload schema

The backend currently sends APNs `aps` payloads with custom keys for iOS.
For Android, the backend must include these as **data** fields (not just
`notification` fields) so we can read them in any app state:

```json
{
  "to": "FCM_TOKEN_HERE",
  "notification": {
    "title": "New bid received",
    "body": "John Doe bid 50000 ₽ on your dump truck order"
  },
  "data": {
    "type": "new_bid",
    "orderId": "abc-uuid",
    "offerId": "def-uuid",
    "bidId": "def-uuid",
    "notificationId": "ghi-uuid",
    "title": "New bid received",
    "body": "John Doe bid 50000 ₽ on your dump truck order"
  }
}
```

The `title`/`body` are duplicated in `data` for foreground delivery — the
`notification` block only fires the system tray when the app is in
background/killed state.

## FcmService

```kotlin
class FcmService : FirebaseMessagingService() {

    @Inject lateinit var notificationStore: NotificationStore
    @Inject lateinit var pushRepo: PushRepository

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("[push] New FCM token")
        applicationScope.launch { pushRepo.registerToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val parsed = AppNotification.fromFcmData(message.data, message.notification) ?: return
        notificationStore.add(parsed)
        showSystemNotification(parsed)
    }

    private fun showSystemNotification(n: AppNotification) {
        val deepLinkUri = Uri.parse("spectech://order/${n.orderId}")
            .buildUpon()
            .apply {
                n.type?.let { appendQueryParameter("type", it) }
                n.offerId?.let { appendQueryParameter("offerId", it) }
            }
            .build()
        val pi = PendingIntent.getActivity(
            this, n.id.hashCode(),
            Intent(Intent.ACTION_VIEW, deepLinkUri).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, "spectech_default")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(n.title)
            .setContentText(n.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(n.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(n.id.hashCode(), builder.build())
        }
    }
}
```

Register in manifest:

```xml
<service
    android:name=".platform.push.FcmService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

## PushRepository

Mirrors `PushRegistrationService`:

```kotlin
@Singleton
class PushRepository @Inject constructor(
    private val api: ApiClient,
    @ApplicationContext private val ctx: Context,
) {
    private var lastRegisteredToken: String? = null

    suspend fun registerIfNeeded() {
        val token = runCatching {
            FirebaseMessaging.getInstance().token.await()
        }.getOrNull() ?: return
        registerToken(token)
    }

    suspend fun registerToken(token: String) {
        if (token == lastRegisteredToken) return
        runCatching {
            api.send<RegisterTokenResponse>(PushApi.Register(RegisterTokenRequest(token, "android")))
            lastRegisteredToken = token
        }.onFailure { Timber.w(it, "Failed to register FCM token") }
    }

    suspend fun unregister() {
        val token = lastRegisteredToken
            ?: runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
            ?: return
        runCatching {
            api.send<UnregisterTokenResponse>(PushApi.Unregister(UnregisterTokenRequest(token)))
            lastRegisteredToken = null
        }.onFailure { Timber.w(it, "Failed to unregister FCM token") }
    }
}
```

Important: `registerToken` must be called **after** sign-in (the backend
binds the token to the current user). On Android, hook this into the
`SessionStore.currentSession` flow:

```kotlin
// In SpecTechApplication.onCreate or a Hilt singleton observer
applicationScope.launch {
    sessionStore.currentSession.collect { session ->
        if (session != null) pushRepo.registerIfNeeded()
        else pushRepo.unregister()
    }
}
```

## NotificationStore (already covered)

See [03-domain/06-news-and-notification.md](../03-domain/06-news-and-notification.md).
Key methods:
- `add(notification)` — dedup, prepend, persist
- `markRead(id)` / `markAllRead()` / `clear()`
- `requestNavigation(req)` — sets `navigationRequest` for the navigator to pick up
- `finishNavigationRequest(req)` — clears it once handled
- `unreadCount` — drives the badge

## NotificationsView (UI)

`LazyColumn` of notifications. Each row:
- Bell icon (filled if unread, outlined if read)
- Title (bold)
- Body
- Relative date
- Tap → mark read, request navigation, dismiss sheet

```kotlin
@Composable
fun NotificationsSheet(onDismiss: () -> Unit) {
    val store = LocalNotificationStore.current
    val list by store.notifications.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    Scaffold(
        topBar = { TopAppBar(
            title = { Text(stringResource(R.string.notifications)) },
            actions = {
                TextButton(onClick = { store.markAllRead() }) { Text(stringResource(R.string.mark_all_read)) }
                TextButton(onClick = { store.clear() }) { Text(stringResource(R.string.clear_all)) }
            },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null)
                }
            },
        )}
    ) { padding ->
        if (list.isEmpty()) EmptyStateView(R.string.notifications_empty)
        else LazyColumn(Modifier.padding(padding)) {
            items(list, key = { it.id.toString() }) { n ->
                NotificationRow(n, onClick = {
                    store.markRead(n.id)
                    if (!n.orderId.isNullOrEmpty()) {
                        store.requestNavigation(NotificationNavigationRequest(
                            type = n.type, orderId = n.orderId, offerId = n.offerId ?: n.bidId
                        ))
                    }
                    onDismiss()
                })
            }
        }
    }
}
```

## Deep-link handling from a tap

When a notification is tapped while the app is killed/background, Android
fires `MainActivity.onCreate(intent)` or `onNewIntent(intent)`. The intent's
data URI is the deep link we set in `setContentIntent`.

```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleNotificationIntent(intent)
}

private fun handleNotificationIntent(intent: Intent?) {
    val data = intent?.data ?: return
    if (data.scheme != "spectech" || data.host != "order") return
    val orderId = data.lastPathSegment ?: return
    notificationStore.requestNavigation(NotificationNavigationRequest(
        type = data.getQueryParameter("type"),
        orderId = orderId,
        offerId = data.getQueryParameter("offerId"),
    ))
}
```

## Navigation processor

A composable that lives at the top of `MainTabsScreen` watches the store
and routes:

```kotlin
@Composable
fun NotificationNavigator(tabNavControllers: Map<Tabs, NavController>) {
    val store = LocalNotificationStore.current
    val req by store.navigationRequest.collectAsStateWithLifecycle()

    LaunchedEffect(req?.id) {
        val r = req ?: return@LaunchedEffect
        try {
            val (tab, scope) = notificationTargetTab(r.type)
            // Switch to the right tab
            switchTab(tab)
            // Fetch orders to find the target
            val orders = ordersRepo.fetchOrders(scope)
            val order = orders.firstOrNull { it.id.toString() == r.orderId } ?: return@LaunchedEffect
            // Push onto that tab's nav stack
            tabNavControllers[tab]?.navigate(OrderDetailRoute(order.id.toString())) {
                popUpTo<MarketplaceRoute>(inclusive = false)  // reset stack to root
            }
        } finally {
            store.finishNavigationRequest(r)
        }
    }
}
```

## Permission

On API 33+, `POST_NOTIFICATIONS` is a runtime permission. Request at app
start (already shown in [02-architecture/06-app-startup.md](../02-architecture/06-app-startup.md)):

```kotlin
val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ignore */ }
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
}
```

Also request before enabling the saved-filter notification opt-in (see
[13-saved-filters.md](13-saved-filters.md)).
