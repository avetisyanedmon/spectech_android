package com.spectech.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ContractorContact(
    val phone: String? = null,
    val name: String? = null,
)
