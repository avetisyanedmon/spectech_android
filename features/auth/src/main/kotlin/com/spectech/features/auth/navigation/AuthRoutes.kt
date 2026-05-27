package com.spectech.features.auth.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AuthRoute {
    @Serializable data object Start : AuthRoute
    @Serializable data object Register : AuthRoute
    @Serializable data class VerifyOtp(val phone: String) : AuthRoute
}
