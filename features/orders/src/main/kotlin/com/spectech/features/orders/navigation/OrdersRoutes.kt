package com.spectech.features.orders.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface MyOrdersRoute {
    @Serializable data object List : MyOrdersRoute
    @Serializable data class Detail(val orderId: String) : MyOrdersRoute
}

@Serializable
sealed interface MyBidsRoute {
    @Serializable data object List : MyBidsRoute
    @Serializable data class Detail(val orderId: String) : MyBidsRoute
}
