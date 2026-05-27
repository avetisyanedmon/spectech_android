package com.spectech.features.marketplace.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
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
 */
@Composable
fun MarketplaceNavGraph(
    paddingValues: PaddingValues = PaddingValues(),
    onSubmitBidRequested: (Order) -> Unit = {},
) {
    val nav = rememberNavController()
    val vm: MarketplaceViewModel = hiltViewModel()
    val filters by vm.filters.collectAsStateWithLifecycle()

    NavHost(nav, startDestination = MarketplaceRoute.List) {
        composable<MarketplaceRoute.List> {
            MarketplaceListScreen(
                onOrderClick = { id -> nav.navigate(MarketplaceRoute.Detail(id)) },
                onOpenFilters = { vm.showingFilters = true },
                paddingValues = paddingValues,
                viewModel = vm,
            )
        }
        composable<MarketplaceRoute.Detail> { entry ->
            val args = entry.toRoute<MarketplaceRoute.Detail>()
            OrderDetailScreen(
                orderId = args.orderId,
                onSubmitBid = { order -> onSubmitBidRequested(order) },
                paddingValues = paddingValues,
                viewModel = vm,
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
