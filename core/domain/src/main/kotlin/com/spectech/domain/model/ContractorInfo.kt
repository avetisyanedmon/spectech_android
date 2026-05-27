package com.spectech.domain.model

import kotlinx.serialization.Serializable

/**
 * Optional nested contractor block embedded in bid responses. Some backends
 * return it as `contractor`, `user`, or `submittedBy` (see iOS `Bid` for the
 * resolution order).
 */
@Serializable
data class ContractorInfo(
    val id: String? = null,
    val phone: String? = null,
    val name: String? = null,
    val email: String? = null,
)
