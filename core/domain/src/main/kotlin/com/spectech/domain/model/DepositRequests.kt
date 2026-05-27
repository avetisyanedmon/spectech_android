package com.spectech.domain.model

import kotlinx.serialization.Serializable

/** `POST /payments/deposits` body. */
@Serializable
data class CreateDepositRequest(
    val equipmentId: String,
    val returnUrl: String? = null,
    val savePaymentMethod: Boolean = false,
)

/** `POST /payments/deposits/{id}/refund` body. */
@Serializable
data class RefundDepositRequest(
    val reason: String? = null,
)

/**
 * `GET /payments/deposits/equipment/{id}` response. The backend always sets
 * [equipmentId]; [deposit] is null when no active deposit exists for the
 * equipment yet. [isPaid] is a server-side convenience flag that's true iff
 * `deposit?.status == PAID`.
 */
@Serializable
data class DepositForEquipmentPayload(
    val equipmentId: String,
    val deposit: Deposit? = null,
    val isPaid: Boolean = false,
)
