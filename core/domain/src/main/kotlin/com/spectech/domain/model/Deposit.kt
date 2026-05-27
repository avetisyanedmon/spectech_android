package com.spectech.domain.model

import com.spectech.domain.enums.DepositStatus
import com.spectech.domain.serializers.BigDecimalSerializer
import java.math.BigDecimal
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Performance security deposit attached to an equipment unit. Mirrors iOS
 * `Deposit` (SpecTechIOS/Shared/Models/DomainModels.swift).
 *
 *   - `confirmationUrl` is the YooKassa-hosted page the contractor must open
 *     to finish payment. Set only when [status] == [DepositStatus.PENDING].
 *   - Each terminal-state instant ([paidAt], [refundedAt], [forfeitedAt]) is
 *     populated by the backend webhook when the lifecycle transitions.
 */
@Serializable
data class Deposit(
    val id: String,
    val equipmentId: String,
    val contractorId: String,
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
    val currency: String,
    val status: DepositStatus,
    val confirmationUrl: String? = null,
    val paymentMethodId: String? = null,
    val paidAt: Instant? = null,
    val refundedAt: Instant? = null,
    val forfeitedAt: Instant? = null,
    val forfeitedToUserId: String? = null,
    val failureReason: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
