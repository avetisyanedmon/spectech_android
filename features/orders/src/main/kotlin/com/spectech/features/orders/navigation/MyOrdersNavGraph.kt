package com.spectech.features.orders.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.spectech.data.events.TabReselection
import com.spectech.data.notifications.NotificationStore
import com.spectech.features.orders.ui.MyOrderDetailScreen
import com.spectech.features.orders.ui.MyOrdersListScreen
import com.spectech.features.orders.viewmodel.MyOrdersViewModel

/**
 * MyOrders tab — graph-level VM so the list cache survives navigating into
 * the order detail and back. Mirrors how the marketplace tab does it.
 *
 * Subscribes to [NotificationStore.navigationRequest] and pushes the detail
 * screen when the request targets this tab (e.g. `new_bid` / `offer_created`
 * pushes that fire when a contractor places a bid on the customer's own
 * order). Acks the request via `finishNavigationRequest` so the sibling
 * MyBids / Marketplace graphs ignore it.
 */
@Composable
fun MyOrdersNavGraph(
    paddingValues: PaddingValues = PaddingValues(),
    notificationStore: NotificationStore = hiltViewModel<MyOrdersNavGraphAccessor>().notificationStore,
) {
    val accessor: MyOrdersNavGraphAccessor = hiltViewModel()
    val nav = rememberNavController()
    val vm: MyOrdersViewModel = hiltViewModel()
    val navigationRequest by notificationStore.navigationRequest.collectAsStateWithLifecycle()

    LaunchedEffect(navigationRequest?.id) {
        val r = navigationRequest ?: return@LaunchedEffect
        if (!isMyOrdersTarget(r.type)) return@LaunchedEffect
        if (r.orderId.isBlank()) return@LaunchedEffect
        nav.navigate(MyOrdersRoute.Detail(r.orderId)) { launchSingleTop = true }
        notificationStore.finishNavigationRequest(r)
    }

    // Pop to the MyOrders root when the user re-taps this tab — mirrors
    // iOS TabView's standard behaviour.
    LaunchedEffect(Unit) {
        accessor.tabReselectionBus.tappedTab.collect { id ->
            if (id == TabReselection.MY_ORDERS) {
                nav.popBackStack(nav.graph.findStartDestination().id, inclusive = false)
            }
        }
    }

    NavHost(nav, startDestination = MyOrdersRoute.List) {
        composable<MyOrdersRoute.List> {
            MyOrdersListScreen(
                onOrderClick = { id -> nav.navigate(MyOrdersRoute.Detail(id)) },
                paddingValues = paddingValues,
                viewModel = vm,
            )
        }
        composable<MyOrdersRoute.Detail> { entry ->
            val args = entry.toRoute<MyOrdersRoute.Detail>()
            MyOrderDetailScreen(
                orderId = args.orderId,
                paddingValues = paddingValues,
                viewModel = vm,
            )
        }
    }
}

/**
 * Notification types that should land on a MyOrders detail. Mirrors iOS'
 * `notificationTargetTab` mapping for these two types — both fire on the
 * customer side when a contractor places / creates a bid.
 */
private fun isMyOrdersTarget(type: String?): Boolean = when (type) {
    "new_bid", "offer_created" -> true
    else -> false
}

/**
 * Tiny VM whose only purpose is to expose [NotificationStore] into this
 * composable graph via Hilt — feature graphs don't have a `LocalContext`
 * shortcut for singletons, so we lean on the existing ViewModel DI pipeline.
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class MyOrdersNavGraphAccessor @javax.inject.Inject constructor(
    val notificationStore: NotificationStore,
    val tabReselectionBus: com.spectech.data.events.TabReselectionBus,
) : androidx.lifecycle.ViewModel()
