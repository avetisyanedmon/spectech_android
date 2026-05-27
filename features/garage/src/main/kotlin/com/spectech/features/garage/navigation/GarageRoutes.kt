package com.spectech.features.garage.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface GarageRoute {
    @Serializable data object List : GarageRoute
    @Serializable data class Detail(val equipmentId: String) : GarageRoute
}
