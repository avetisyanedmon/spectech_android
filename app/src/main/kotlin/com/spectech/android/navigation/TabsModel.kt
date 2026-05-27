package com.spectech.android.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.ContentPasteSearch
import androidx.compose.material.icons.outlined.Garage
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.ui.graphics.vector.ImageVector
import com.spectech.android.R
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable

/**
 * Tab routes for the root NavHost. Marker sealed type so each route is also a
 * valid type-safe Navigation Compose destination.
 *
 * Mirrors the iOS `MainTab` enum in SpecTechIOS/Scene/Root/MainTabView.swift.
 */
sealed interface TabRoute

@Serializable data object MarketplaceTab : TabRoute
@Serializable data object MyBidsTab : TabRoute
@Serializable data object MyOrdersTab : TabRoute
@Serializable data object GarageTab : TabRoute
@Serializable data object NewsTab : TabRoute

/** Metadata for rendering a tab in the bottom nav. */
data class TabSpec(
    val route: TabRoute,
    val routeClass: KClass<out TabRoute>,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
    val requiresAuth: Boolean,
)

/**
 * Order matches iOS: Announcements → My Bids → My Orders → Garage → News.
 * Icon choices map SF Symbols to their closest Material counterpart per
 * docs/06-ui/05-images-and-icons.md.
 */
val SpecTechTabs: List<TabSpec> = listOf(
    TabSpec(
        route = MarketplaceTab,
        routeClass = MarketplaceTab::class,
        icon = Icons.Outlined.Inventory2,
        labelRes = R.string.tab_marketplace,
        requiresAuth = false,
    ),
    TabSpec(
        route = MyBidsTab,
        routeClass = MyBidsTab::class,
        icon = Icons.Outlined.AssignmentTurnedIn,
        labelRes = R.string.tab_my_bids,
        requiresAuth = true,
    ),
    TabSpec(
        route = MyOrdersTab,
        routeClass = MyOrdersTab::class,
        icon = Icons.Outlined.ContentPasteSearch,
        labelRes = R.string.tab_my_orders,
        requiresAuth = true,
    ),
    TabSpec(
        route = GarageTab,
        routeClass = GarageTab::class,
        icon = Icons.Outlined.Garage,
        labelRes = R.string.tab_garage,
        requiresAuth = true,
    ),
    TabSpec(
        route = NewsTab,
        routeClass = NewsTab::class,
        icon = Icons.Outlined.Newspaper,
        labelRes = R.string.tab_news,
        requiresAuth = false,
    ),
)

/** Top-bar title key for a given tab (separate from the tiny bottom-bar label). */
@StringRes
fun TabRoute.topBarTitleRes(): Int = when (this) {
    MarketplaceTab -> R.string.tab_title_marketplace
    MyBidsTab -> R.string.tab_title_my_bids
    MyOrdersTab -> R.string.tab_title_my_orders
    GarageTab -> R.string.tab_title_garage
    NewsTab -> R.string.tab_title_news
}
