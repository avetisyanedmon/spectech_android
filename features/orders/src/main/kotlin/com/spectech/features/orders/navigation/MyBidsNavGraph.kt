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
import com.spectech.features.orders.ui.MyBidDetailScreen
import com.spectech.features.orders.ui.MyBidsListScreen
import com.spectech.features.orders.viewmodel.MyBidsViewModel

/**
 * MyBids tab — same nested-NavHost pattern as MyOrders. Subscribes to
 * [NotificationStore.navigationRequest] and pushes the detail screen for
 * `bid_accepted` / `offer_accepted` / `offer_rejected` / `bid_rejected`
 * pushes (everything that fires on the contractor side when the customer
 * acts on their bid). Acks the request so the sibling MyOrders / Marketplace
 * graphs ignore it.
 */
@Composable
fun MyBidsNavGraph(
    paddingValues: PaddingValues = PaddingValues(),
    notificationStore: NotificationStore = hiltViewModel<MyBidsNavGraphAccessor>().notificationStore,
) {
    val accessor: MyBidsNavGraphAccessor = hiltViewModel()
    val nav = rememberNavController()
    val vm: MyBidsViewModel = hiltViewModel()
    val navigationRequest by notificationStore.navigationRequest.collectAsStateWithLifecycle()

    LaunchedEffect(navigationRequest?.id) {
        val r = navigationRequest ?: return@LaunchedEffect
        if (!isMyBidsTarget(r.type)) return@LaunchedEffect
        if (r.orderId.isBlank()) return@LaunchedEffect
        nav.navigate(MyBidsRoute.Detail(r.orderId)) { launchSingleTop = true }
        notificationStore.finishNavigationRequest(r)
    }

    // Pop to the MyBids root on tab re-tap.
    LaunchedEffect(Unit) {
        accessor.tabReselectionBus.tappedTab.collect { id ->
            if (id == TabReselection.MY_BIDS) {
                nav.popBackStack(nav.graph.findStartDestination().id, inclusive = false)
            }
        }
    }

    NavHost(nav, startDestination = MyBidsRoute.List) {
        composable<MyBidsRoute.List> {
            MyBidsListScreen(
                onOrderClick = { id -> nav.navigate(MyBidsRoute.Detail(id)) },
                paddingValues = paddingValues,
                viewModel = vm,
            )
        }
        composable<MyBidsRoute.Detail> { entry ->
            val args = entry.toRoute<MyBidsRoute.Detail>()
            MyBidDetailScreen(
                orderId = args.orderId,
                paddingValues = paddingValues,
                viewModel = vm,
            )
        }
    }
}

/**
 * Notification types that should land on a MyBids detail. Mirrors iOS'
 * `notificationTargetTab` mapping for these types — all fire on the
 * contractor side as a reaction to the customer accepting or rejecting
 * their bid.
 */
private fun isMyBidsTarget(type: String?): Boolean = when (type) {
    "bid_accepted", "offer_accepted", "offer_rejected", "bid_rejected" -> true
    else -> false
}

/**
 * Tiny VM whose only purpose is to expose [NotificationStore] into this
 * composable graph via Hilt — feature graphs don't have a `LocalContext`
 * shortcut for singletons, so we lean on the existing ViewModel DI pipeline.
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class MyBidsNavGraphAccessor @javax.inject.Inject constructor(
    val notificationStore: NotificationStore,
    val tabReselectionBus: com.spectech.data.events.TabReselectionBus,
) : androidx.lifecycle.ViewModel()
