package com.spectech.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Performance security deposit lifecycle. Only `paid` lets a contractor bid
 * with the equipment unit. Mirrors iOS `DepositStatus`
 * (SpecTechIOS/Shared/Models/DomainModels.swift).
 */
@Serializable
enum class DepositStatus(val wire: String) {
    @SerialName("pending") PENDING("pending"),
    @SerialName("paid") PAID("paid"),
    @SerialName("failed") FAILED("failed"),
    @SerialName("refund_pending") REFUND_PENDING("refund_pending"),
    @SerialName("refunded") REFUNDED("refunded"),
    @SerialName("forfeited") FORFEITED("forfeited");

    /** Unlocks bid submission for the contractor. */
    val isActiveAndPaid: Boolean get() = this == PAID

    /** A new deposit can't be created while one of these is active. */
    val blocksNewDeposit: Boolean
        get() = this in setOf(PENDING, PAID, REFUND_PENDING)
}
