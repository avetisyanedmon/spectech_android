package com.spectech.android.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spectech.android.BuildConfig
import com.spectech.android.R
import com.spectech.android.ui.sheets.NotificationsSheet
import com.spectech.android.ui.sheets.ProfileSheet
import com.spectech.android.ui.tabs.GarageTabPlaceholder
import com.spectech.android.ui.tabs.MyBidsTabPlaceholder
import com.spectech.android.ui.tabs.MyOrdersTabPlaceholder
import com.spectech.data.auth.SessionStore
import com.spectech.data.events.TabReselection
import com.spectech.data.events.TabReselectionBus
import com.spectech.data.notifications.NotificationStore
import com.spectech.domain.model.Order
import com.spectech.features.auth.ui.AuthFlow
import com.spectech.features.bidding.ui.BidSheet
import com.spectech.features.createOrder.ui.CreateOrderSheet
import com.spectech.features.garage.navigation.GarageNavGraph
import com.spectech.features.marketplace.navigation.MarketplaceNavGraph
import com.spectech.features.orders.navigation.MyBidsNavGraph
import com.spectech.features.orders.navigation.MyOrdersNavGraph
import com.spectech.features.news.ui.NewsScreen
import com.spectech.features.profile.ui.EditProfileSheet
import com.spectech.features.profile.ui.LanguagePickerSheet
import com.spectech.features.support.ui.SupportChatSheet
import kotlinx.coroutines.launch

/**
 * Top-level routing screen. Mirrors iOS `MainTabView`.
 *
 *   ┌────────────────────────────────────────────────────────────┐
 *   │ [Support]      Active orders        [+]/[Sign In] [🔔] [👤] │
 *   ├────────────────────────────────────────────────────────────┤
 *   │                                                            │
 *   │                <per-tab content here>                      │
 *   │                                                            │
 *   ├────────────────────────────────────────────────────────────┤
 *   │  Mp     Bids    Orders   Garage   News                     │
 *   └────────────────────────────────────────────────────────────┘
 *
 * Unauthenticated users see Marketplace + News read-only and a "Sign In" CTA
 * in the top bar; the other tabs render [SignInPromptView]. Tapping any
 * sign-in entry point opens [AuthFlow] in a modal bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabsScreen(
    sessionStore: SessionStore,
    notificationStore: NotificationStore,
) {
    val tabReselectionBus = androidx.hilt.navigation.compose.hiltViewModel<MainTabsAccessor>().tabReselectionBus
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val session by sessionStore.currentSession.collectAsStateWithLifecycle()
    val isAuthenticated = session != null

    val notifications by notificationStore.notifications.collectAsStateWithLifecycle()
    val unreadCount by notificationStore.unreadCount.collectAsStateWithLifecycle()
    val navigationRequest by notificationStore.navigationRequest.collectAsStateWithLifecycle()

    val rootNav = rememberNavController()
    val backStack by rootNav.currentBackStackEntryAsState()
    val currentDestination = backStack?.destination

    val activeTabRoute: TabRoute = remember(currentDestination) {
        SpecTechTabs.firstOrNull { tab ->
            currentDestination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
        }?.route ?: MarketplaceTab
    }

    // Sheet state — `rememberSaveable` so rotation doesn't reopen accidental sheets
    var showAuthSheet by rememberSaveable { mutableStateOf(false) }
    var showProfileSheet by rememberSaveable { mutableStateOf(false) }
    var showEditProfileSheet by rememberSaveable { mutableStateOf(false) }
    var showLanguageSheet by rememberSaveable { mutableStateOf(false) }
    var showSupportSheet by rememberSaveable { mutableStateOf(false) }
    var showNotificationsSheet by rememberSaveable { mutableStateOf(false) }
    var showCreateOrderSheet by rememberSaveable { mutableStateOf(false) }
    var showMarketplaceFilters by remember { mutableStateOf(false) }
    // Bid sheet target — `remember` (not `rememberSaveable`) because Order isn't @Parcelable.
    var bidSheetOrder by remember { mutableStateOf<Order?>(null) }

    // Auto-dismiss auth sheet on successful sign in
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated && showAuthSheet) showAuthSheet = false
    }

    // Deep-link from a push tap or in-app notification row. Switches to the
    // relevant tab, dismisses the notifications sheet, and lets the per-tab
    // nav graph (MarketplaceNavGraph / MyOrdersNavGraph / MyBidsNavGraph)
    // push the order detail onto its own back stack. Each of those graphs
    // owns the `finishNavigationRequest` ack so we don't race them here —
    // a short fallback delay catches request types no graph claims so a
    // stale request can't lock the deep-link pipeline.
    LaunchedEffect(navigationRequest?.id) {
        val req = navigationRequest ?: return@LaunchedEffect
        val target = notificationTargetTab(req.type)
        if (activeTabRoute != target) {
            rootNav.navigate(target) {
                popUpTo(rootNav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        showNotificationsSheet = false
        kotlinx.coroutines.delay(NAV_REQ_FALLBACK_ACK_MS)
        // If a child graph already acked, this is a no-op — the store
        // compares the id before clearing.
        notificationStore.finishNavigationRequest(req)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(activeTabRoute.topBarTitleRes())) },
                navigationIcon = {
                    IconButton(onClick = { showSupportSheet = true }) {
                        Icon(
                            imageVector = Icons.Outlined.SupportAgent,
                            contentDescription = stringResource(R.string.support_title),
                        )
                    }
                },
                actions = {
                    // Filter button on Marketplace tab
                    if (activeTabRoute == MarketplaceTab) {
                        IconButton(onClick = { showMarketplaceFilters = true }) {
                            Icon(
                                imageVector = Icons.Outlined.FilterAlt,
                                contentDescription = stringResource(R.string.filters_title),
                            )
                        }
                    }

                    // "Create order" affordance on both the Marketplace and
                    // My Orders tabs — matches iOS `MyOrdersView`'s primaryAction
                    // toolbar item which only appears when authenticated.
                    val showsCreateOrderAffordance =
                        activeTabRoute == MarketplaceTab || activeTabRoute == MyOrdersTab
                    if (showsCreateOrderAffordance) {
                        if (isAuthenticated) {
                            IconButton(onClick = { showCreateOrderSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.action_create_order),
                                )
                            }
                        } else if (activeTabRoute == MarketplaceTab) {
                            // The My Orders tab already shows the auth wall via
                            // its placeholder, so the sign-in chip is only
                            // useful on the marketplace tab.
                            TextButton(onClick = { showAuthSheet = true }) {
                                Text(
                                    stringResource(com.spectech.uikit.R.string.sign_in),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                    if (isAuthenticated) {
                        IconButton(onClick = { showNotificationsSheet = true }) {
                            androidx.compose.material3.BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        androidx.compose.material3.Badge {
                                            Text(
                                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                            )
                                        }
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = stringResource(R.string.notifications_title),
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showProfileSheet = true }) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = stringResource(R.string.profile_title),
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                SpecTechTabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (selected) {
                                // Tap-already-selected → pop the nested tab's
                                // back stack to its start. iOS' TabView does
                                // this for free; on Android we route through
                                // [TabReselectionBus] so each per-tab graph
                                // pops itself (the root NavController doesn't
                                // own the nested back stacks).
                                coroutineScope.launch {
                                    tabReselectionBus.emit(tabReselectionId(tab.route))
                                }
                            } else {
                                rootNav.navigate(tab.route) {
                                    popUpTo(rootNav.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = {
                            Text(
                                text = stringResource(tab.labelRes),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        // Inner padding is consumed by each placeholder so the content respects
        // the top/bottom bars without us having to pass insets to every screen.
        TabsNavHost(
            navController = rootNav,
            innerPadding = padding,
            isAuthenticated = isAuthenticated,
            onSignIn = { showAuthSheet = true },
            onSubmitBidRequested = { order -> bidSheetOrder = order },
        )
    }

    if (showAuthSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showAuthSheet = false }, sheetState = sheetState) {
            AuthFlow(onSignedIn = { showAuthSheet = false })
        }
    }
    if (showProfileSheet) {
        ProfileSheet(
            versionName = BuildConfig.VERSION_NAME,
            unreadNotificationCount = unreadCount,
            onSignIn = {
                showProfileSheet = false
                showAuthSheet = true
            },
            onOpenEdit = {
                showProfileSheet = false
                showEditProfileSheet = true
            },
            onOpenLanguage = {
                showProfileSheet = false
                showLanguageSheet = true
            },
            onOpenNotifications = {
                showProfileSheet = false
                showNotificationsSheet = true
            },
            onDismiss = { showProfileSheet = false },
        )
    }
    if (showEditProfileSheet) {
        EditProfileSheet(onDismiss = { showEditProfileSheet = false })
    }
    if (showLanguageSheet) {
        LanguagePickerSheet(onDismiss = { showLanguageSheet = false })
    }
    if (showSupportSheet) {
        SupportChatSheet(onDismiss = { showSupportSheet = false })
    }
    if (showNotificationsSheet) {
        NotificationsSheet(
            isAuthenticated = isAuthenticated,
            notifications = notifications,
            onSignIn = {
                showNotificationsSheet = false
                showAuthSheet = true
            },
            onDismiss = { showNotificationsSheet = false },
            onMarkAllRead = { notificationStore.markAllRead() },
            onClearAll = { notificationStore.clear() },
            onMarkRead = { entry -> notificationStore.markRead(entry.id) },
            onTap = { tapped ->
                notificationStore.markRead(tapped.id)
                val orderId = tapped.orderId
                if (!orderId.isNullOrBlank()) {
                    showNotificationsSheet = false
                    notificationStore.requestNavigation(
                        com.spectech.domain.model.NotificationNavigationRequest(
                            id = java.util.UUID.randomUUID().toString(),
                            type = tapped.type,
                            orderId = orderId,
                            offerId = tapped.offerId ?: tapped.bidId,
                        ),
                    )
                }
            },
        )
    }
    if (showCreateOrderSheet) {
        CreateOrderSheet(onDismiss = { showCreateOrderSheet = false })
    }
    bidSheetOrder?.let { order ->
        BidSheet(
            order = order,
            onDismiss = { bidSheetOrder = null },
            onRequestAddEquipment = {
                bidSheetOrder = null
                rootNav.navigate(GarageTab) {
                    popUpTo(rootNav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
    }
}

@Composable
private fun TabsNavHost(
    navController: androidx.navigation.NavHostController,
    innerPadding: PaddingValues,
    isAuthenticated: Boolean,
    onSignIn: () -> Unit,
    onSubmitBidRequested: (Order) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = MarketplaceTab,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        composable<MarketplaceTab> {
            MarketplaceNavGraph(
                paddingValues = PaddingValues(0.dp),
                onSubmitBidRequested = onSubmitBidRequested,
                onSignInRequested = onSignIn,
                filterButtonClicked = showMarketplaceFilters,
                onFilterShown = { showMarketplaceFilters = false },
            )
        }
        composable<MyBidsTab> {
            if (isAuthenticated) {
                MyBidsNavGraph(paddingValues = PaddingValues(0.dp))
            } else {
                MyBidsTabPlaceholder(
                    isAuthenticated = false,
                    onSignIn = onSignIn,
                    padding = PaddingValues(0.dp),
                )
            }
        }
        composable<MyOrdersTab> {
            if (isAuthenticated) {
                MyOrdersNavGraph(paddingValues = PaddingValues(0.dp))
            } else {
                MyOrdersTabPlaceholder(
                    isAuthenticated = false,
                    onSignIn = onSignIn,
                    padding = PaddingValues(0.dp),
                )
            }
        }
        composable<GarageTab> {
            if (isAuthenticated) {
                GarageNavGraph(
                    paddingValues = PaddingValues(0.dp),
                    onSignInRequested = onSignIn,
                )
            } else {
                GarageTabPlaceholder(
                    isAuthenticated = false,
                    onSignIn = onSignIn,
                    padding = PaddingValues(0.dp),
                )
            }
        }
        composable<NewsTab> {
            NewsScreen(paddingValues = PaddingValues(0.dp))
        }
    }
}

/**
 * Maps a backend notification `type` to the tab a tap should land on. Mirrors
 * the iOS routing in `NotificationStore`:
 *   - bids on orders I created → My Orders
 *   - updates on bids I submitted → My Bids
 *   - new orders matching my equipment → Marketplace
 * Anything unrecognised falls back to My Orders, which matches the most
 * common "someone acted on my stuff" intent.
 */
private fun notificationTargetTab(type: String?): TabRoute = when (type) {
    "new_bid", "offer_created" -> MyOrdersTab
    "bid_accepted", "offer_accepted", "offer_rejected", "bid_rejected" -> MyBidsTab
    "matching_order", "new_matching_order" -> MarketplaceTab
    else -> MyOrdersTab
}

/**
 * Fallback delay (ms) before this composable acks a notification request.
 * The per-tab nav graphs ([MarketplaceNavGraph], [MyOrdersNavGraph],
 * [MyBidsNavGraph]) each subscribe to the same store; whichever one matches
 * the request type pushes its own detail screen and acks first. This
 * timeout is the safety net for request types no graph claims so a stale
 * request can't lock the deep-link pipeline.
 */
private const val NAV_REQ_FALLBACK_ACK_MS = 500L

/**
 * Maps a [TabRoute] to its stable [TabReselection] string id. The bus uses
 * plain strings so feature modules don't have to depend on `:app` to listen.
 */
private fun tabReselectionId(route: TabRoute): String = when (route) {
    MarketplaceTab -> TabReselection.MARKETPLACE
    MyBidsTab -> TabReselection.MY_BIDS
    MyOrdersTab -> TabReselection.MY_ORDERS
    GarageTab -> TabReselection.GARAGE
    NewsTab -> TabReselection.NEWS
}

/**
 * Tiny VM whose only purpose is to expose [TabReselectionBus] into the
 * composable graph via Hilt — the screen itself can't @Inject directly.
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class MainTabsAccessor @javax.inject.Inject constructor(
    val tabReselectionBus: TabReselectionBus,
) : androidx.lifecycle.ViewModel()
