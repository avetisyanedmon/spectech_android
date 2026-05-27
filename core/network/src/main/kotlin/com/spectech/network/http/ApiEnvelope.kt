package com.spectech.network.http

import kotlinx.serialization.Serializable

/**
 * Every backend success response is wrapped in this envelope. See
 * SpecTechIOS/Networking/API/APIRequest.swift.
 */
@Serializable
data class ApiEnvelope<T>(
    val success: Boolean? = null,
    val data: T,
)
