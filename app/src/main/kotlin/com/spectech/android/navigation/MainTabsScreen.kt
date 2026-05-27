package com.spectech.android.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.spectech.android.R
import com.spectech.android.ui.sheets.NotificationsSheet
import com.spectech.android.ui.sheets.ProfileSheet
import com.spectech.android.ui.sheets.SupportSheet
import com.spectech.android.ui.tabs.GarageTabPlaceholder
import com.spectech.android.ui.tabs.MyBidsTabPlaceholder
import com.spectech.android.ui.tabs.MyOrdersTabPlaceholder
import com.spectech.android.ui.tabs.NewsTabPlaceholder
import com.spectech.data.auth.SessionStore
import com.spectech.domain.model.Order
import com.spectech.features.auth.ui.AuthFlow
import com.spectech.features.bidding.ui.BidSheet
import com.spectech.features.createOrder.ui.CreateOrderSheet
import com.spectech.features.garage.navigation.GarageNavGraph
import com.spectech.features.marketplace.navigation.MarketplaceNavGraph
import com.spectech.features.orders.navigation.MyBidsNavGraph
import com.spectech.features.orders.navigation.MyOrdersNavGraph
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
fun MainTabsScreen(sessionStore: SessionStore) {
    val session by sessionStore.currentSession.collectAsStateWithLifecycle()
    val isAuthenticated = session != null
    val scope = rememberCoroutineScope()

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
    var showSupportSheet by rememberSaveable { mutableStateOf(false) }
    var showNotificationsSheet by rememberSaveable { mutableStateOf(false) }
    var showCreateOrderSheet by rememberSaveable { mutableStateOf(false) }
    // Bid sheet target — `remember` (not `rememberSaveable`) because Order isn't @Parcelable.
    var bidSheetOrder by remember { mutableStateOf<Order?>(null) }

    // Auto-dismiss auth sheet on successful sign in
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated && showAuthSheet) showAuthSheet = false
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
                    if (activeTabRoute == MarketplaceTab) {
                        if (isAuthenticated) {
                            IconButton(onClick = { showCreateOrderSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.action_create_order),
                                )
                            }
                        } else {
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
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.notifications_title),
                            )
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
                            rootNav.navigate(tab.route) {
                                popUpTo(rootNav.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
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
            user = session?.user,
            onSignIn = {
                showProfileSheet = false
                showAuthSheet = true
            },
            onLogout = {
                showProfileSheet = false
                scope.launch { sessionStore.clearSession() }
            },
            onDismiss = { showProfileSheet = false },
        )
    }
    if (showSupportSheet) {
        SupportSheet(onDismiss = { showSupportSheet = false })
    }
    if (showNotificationsSheet) {
        NotificationsSheet(
            isAuthenticated = isAuthenticated,
            onSignIn = {
                showNotificationsSheet = false
                showAuthSheet = true
            },
            onDismiss = { showNotificationsSheet = false },
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
                GarageNavGraph(paddingValues = PaddingValues(0.dp))
            } else {
                GarageTabPlaceholder(
                    isAuthenticated = false,
                    onSignIn = onSignIn,
                    padding = PaddingValues(0.dp),
                )
            }
        }
        composable<NewsTab> {
            NewsTabPlaceholder(padding = PaddingValues(0.dp))
        }
    }
}
