# 03 — Navigation

## What iOS does

iOS uses **SwiftUI `NavigationStack(path:)`** with one path per tab.
`MainTabView` owns five `@State` paths:

```swift
@State private var marketplacePath = NavigationPath()
@State private var myBidsPath = NavigationPath()
@State private var myOrdersPath = NavigationPath()
@State private var garagePath = NavigationPath()
// News tab has no inner stack
```

Each tab wraps its root view in a `NavigationStack(path: $tabPath)` and uses
`.navigationDestination(for: Order.self) { OrderDetailView(order: $0) }`.

Sheets are presented from `MainTabView` itself: auth sheet, profile, support,
notifications, create-order, and from order detail: bid sheet.

Deep linking from push notifications uses
`MainTabView.handleNotificationNavigation`: switch tab, then `path.append(order)`.

## Android equivalent: Navigation Compose with type-safe routes

### Top level

```kotlin
@Composable
fun SpecTechApp() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Routes.MainTabs) {
        composable<Routes.MainTabs> { MainTabsScreen(navController) }
        dialog<Routes.AuthSheet> { AuthSheetScreen(navController) }
        dialog<Routes.Profile> { ProfileSheet(navController) }
        dialog<Routes.Support> { SupportSheet(navController) }
        dialog<Routes.Notifications> { NotificationsSheet(navController) }
        dialog<Routes.CreateOrder> { backStack ->
            val args = backStack.toRoute<Routes.CreateOrder>()
            CreateOrderScreen(args.prefillJson, navController)
        }
    }
}
```

`dialog<...>` shows a sheet-style destination (Material 3 has `ModalBottomSheet`
and a full-screen `Dialog`; for the iOS `.sheet` parity use a full-screen
`Dialog` with `usePlatformDefaultWidth = false`).

### Per-tab back stacks

Android does **not** automatically give each tab its own back stack. Use the
official multi-back-stack pattern from `NavigationBar`:

```kotlin
val tabs = listOf(Tabs.Marketplace, Tabs.MyBids, Tabs.MyOrders, Tabs.Garage, Tabs.News)

NavigationBar {
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination
    tabs.forEach { tab ->
        NavigationBarItem(
            selected = current?.hierarchy?.any { it.hasRoute(tab.route::class) } == true,
            onClick = {
                navController.navigate(tab.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(tab.icon, null) },
            label = { Text(stringResource(tab.label)) },
        )
    }
}
```

The `saveState = true` + `restoreState = true` combination preserves each tab's
inner navigation stack across tab switches — same behavior as iOS.

### Type-safe routes

Use `@Serializable` data classes for every destination — Compose 1.7+ supports
this natively:

```kotlin
@Serializable sealed interface Routes {
    @Serializable data object MainTabs : Routes
    @Serializable data object AuthSheet : Routes
    @Serializable data object Profile : Routes
    @Serializable data object Support : Routes
    @Serializable data object Notifications : Routes
    @Serializable data class CreateOrder(val prefillJson: String? = null) : Routes
}

@Serializable sealed interface Tabs {
    val route: Any
    val icon: ImageVector
    val label: Int
    @Serializable data object Marketplace : Tabs { override val route = MarketplaceRoute }
    // …
}

@Serializable data object MarketplaceRoute
@Serializable data class OrderDetailRoute(val orderId: String)
@Serializable data object GarageRoute
@Serializable data class EquipmentDetailRoute(val equipmentId: String)
```

Each tab is its own `NavHost` (nested):

```kotlin
@Composable
fun MarketplaceNavGraph() {
    val tabNav = rememberNavController()
    NavHost(tabNav, startDestination = MarketplaceRoute) {
        composable<MarketplaceRoute> { MarketplaceScreen(onOrderClick = {
            tabNav.navigate(OrderDetailRoute(it.id))
        })}
        composable<OrderDetailRoute> { entry ->
            val args = entry.toRoute<OrderDetailRoute>()
            OrderDetailScreen(args.orderId)
        }
    }
}
```

### Why nested NavHosts (one per tab)

This is the cleanest way to mirror iOS's behavior where each tab has its own
isolated `NavigationStack` and switching tabs preserves the stack. The
alternative — one giant NavHost with `popUpTo`/`saveState` — works but is
harder to reason about with five tabs.

### Sheet vs full-screen

| iOS | Android |
|---|---|
| `.sheet` covering most of the screen | `ModalBottomSheet` if drag-dismiss matters, otherwise full-screen `Dialog` |
| Sheet with internal `NavigationStack` (auth flow) | Full-screen destination with its own nested `NavHost` |
| `.fullScreenCover` | Composable destination at top-level |

The iOS **Auth sheet** is a sheet with an internal `NavigationStack`:
start → register → verifyOtp. On Android, present `AuthSheet` as a full-screen
destination with its own nested `NavHost(startDestination = AuthRoutes.Start)`.

### Deep linking from notifications

```kotlin
// In FcmService, after parsing AppNotification:
val deepLink = "spectech://order/${notification.orderId}?type=${notification.type ?: ""}"

val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
    setPackage(context.packageName)
}
val pending = PendingIntent.getActivity(context, 0, intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
```

Register the URI scheme in `MainActivity` manifest:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="spectech" android:host="order" />
</intent-filter>
```

In `NavHost`:

```kotlin
composable<OrderDetailRoute>(
    deepLinks = listOf(navDeepLink { uriPattern = "spectech://order/{orderId}?type={type}" })
) { … }
```

The notification handler logic that maps push types to tabs lives in iOS at
`MainTabView.notificationTarget(for:)`. Port to Kotlin:

```kotlin
fun notificationTargetTab(type: String?): Pair<Tabs, OrderScope> = when (type) {
    "new_bid", "offer_created" -> Tabs.MyOrders to OrderScope.MINE
    "bid_accepted", "offer_accepted", "offer_rejected" -> Tabs.MyBids to OrderScope.PENDING
    "matching_order", "new_matching_order" -> Tabs.Marketplace to OrderScope.MARKETPLACE
    else -> Tabs.Marketplace to OrderScope.ALL
}
```

After receiving a deep-link, switch to the target tab and navigate to the order
detail within that tab's nested NavHost.
