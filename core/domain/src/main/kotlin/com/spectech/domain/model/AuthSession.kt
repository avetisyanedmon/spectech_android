package com.spectech.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthSession(
    val token: String,
    val user: User,
    val isNewUser: Boolean,
)
