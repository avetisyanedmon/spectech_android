package com.spectech.features.marketplace.navigation

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
import com.spectech.domain.model.Order
import com.spectech.features.marketplace.filters.MarketplaceFilterSheet
import com.spectech.features.marketplace.ui.MarketplaceListScreen
import com.spectech.features.marketplace.ui.OrderDetailScreen
import com.spectech.features.marketplace.viewmodel.MarketplaceViewModel

/**
 * Self-contained graph for the Marketplace tab. Owns its own NavHostController
 * and the filter sheet state. The parent (`MainTabsScreen`) just composes
 * this into the `composable<MarketplaceTab>` slot and hands inner padding.
 *
 * The VM is hoisted to this graph level so that both the list screen and
 * the detail screen share the same loaded data — the detail screen reads the
 * order by id out of the VM's cached state instead of refetching.
 *
 * [onSubmitBidRequested] bubbles the "Submit a bid" action up to `:app` so
 * the BidSheet can be presented modally without forcing this module to know
 * about `:features:bidding`. Cross-feature coordination flows through the
 * app entry point per docs/02-architecture/01-module-layout.md.
 *
 * [onSignInRequested] surfaces the "Sign in to participate" sign-in banner
 * tap on the detail screen — same auth-sheet present-from-root pattern as
 * the marketplace top-bar action.
 *
 * Deep-link handling: this graph subscribes to
 * [NotificationStore.navigationRequest] and pushes the order-detail screen
 * onto its own back stack when a notification targets a marketplace order
 * (type `matching_order` / `new_matching_order` / null fallback). The graph
 * acks the request via `finishNavigationRequest` once it has routed so
 * sibling graphs don't re-handle the same one.
 */
@Composable
fun MarketplaceNavGraph(
    paddingValues: PaddingValues = PaddingValues(),
    onSubmitBidRequested: (Order) -> Unit = {},
    onSignInRequested: () -> Unit = {},
    filterButtonClicked: Boolean = false,
    onFilterShown: () -> Unit = {},
    notificationStore: NotificationStore = hiltViewModel<MarketplaceNavGraphAccessor>().notificationStore,
) {
    val accessor: MarketplaceNavGraphAccessor = hiltViewModel()
    val nav = rememberNavController()
    val vm: MarketplaceViewModel = hiltViewModel()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val navigationRequest by notificationStore.navigationRequest.collectAsStateWithLifecycle()

    // Handle filter button clicks from the top bar
    LaunchedEffect(filterButtonClicked) {
        if (filterButtonClicked) {
            vm.showingFilters = true
            onFilterShown()
        }
    }

    // Pop to the marketplace root when the user re-taps this tab on the
    // bottom bar — iOS' TabView does this for free.
    LaunchedEffect(Unit) {
        accessor.tabReselectionBus.tappedTab.collect { id ->
            if (id == TabReselection.MARKETPLACE) {
                nav.popBackStack(nav.graph.findStartDestination().id, inclusive = false)
            }
        }
    }

    // Handle marketplace-scoped deep links — push the detail and ack so other
    // graphs ignore the request.
    LaunchedEffect(navigationRequest?.id) {
        val r = navigationRequest ?: return@LaunchedEffect
        if (!isMarketplaceTarget(r.type)) return@LaunchedEffect
        if (r.orderId.isBlank()) return@LaunchedEffect
        nav.navigate(MarketplaceRoute.Detail(r.orderId)) {
            launchSingleTop = true
        }
        notificationStore.finishNavigationRequest(r)
    }

    NavHost(nav, startDestination = MarketplaceRoute.List) {
        composable<MarketplaceRoute.List> {
            MarketplaceListScreen(
                onOrderClick = { id -> nav.navigate(MarketplaceRoute.Detail(id)) },
                onOpenFilters = { vm.showingFilters = true },
                onSubmitBidRequested = onSubmitBidRequested,
                onSignInRequested = onSignInRequested,
                paddingValues = paddingValues,
                viewModel = vm,
            )
        }
        composable<MarketplaceRoute.Detail> { entry ->
            val args = entry.toRoute<MarketplaceRoute.Detail>()
            OrderDetailScreen(
                orderId = args.orderId,
                onSubmitBid = { order -> onSubmitBidRequested(order) },
                onSignIn = onSignInRequested,
                onClose = { nav.popBackStack() },
                paddingValues = paddingValues,
                marketplaceViewModel = vm,
            )
        }
    }

    if (vm.showingFilters) {
        MarketplaceFilterSheet(
            current = filters,
            onApply = { vm.setFilters(it) },
            onDismiss = { vm.showingFilters = false },
        )
    }
}

/**
 * Notification types that should land on a marketplace order detail. New
 * "matching order" pushes are the obvious ones; we also claim the null /
 * unknown case so any backend type we haven't catalogued yet at least lands
 * the user on the public detail rather than dead-ending.
 */
private fun isMarketplaceTarget(type: String?): Boolean = when (type) {
    "matching_order", "new_matching_order" -> true
    null -> true
    else -> false
}

/**
 * Tiny VM whose only purpose is to expose [NotificationStore] into this
 * composable graph via Hilt — feature graphs don't have a `LocalContext`
 * shortcut for singletons, so we lean on the existing ViewModel DI pipeline.
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class MarketplaceNavGraphAccessor @javax.inject.Inject constructor(
    val notificationStore: NotificationStore,
    val tabReselectionBus: com.spectech.data.events.TabReselectionBus,
) : androidx.lifecycle.ViewModel()
