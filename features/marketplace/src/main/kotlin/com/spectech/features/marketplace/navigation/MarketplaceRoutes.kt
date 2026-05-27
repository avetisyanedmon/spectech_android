package com.spectech.features.marketplace.navigation

import kotlinx.serialization.Serializable

/** Routes inside the marketplace tab's nested NavHost. */
@Serializable
sealed interface MarketplaceRoute {
    @Serializable data object List : MarketplaceRoute
    @Serializable data class Detail(val orderId: String) : MarketplaceRoute
}
