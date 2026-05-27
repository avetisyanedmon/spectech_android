package com.spectech.domain.model

import com.spectech.domain.enums.DepositStatus
import com.spectech.domain.enums.EquipmentCategory
import com.spectech.domain.serializers.NullableEquipmentCategorySerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * One unit of equipment in a contractor's garage. Mirrors iOS `Equipment`
 * (SpecTechIOS/Shared/Models/DomainModels.swift).
 *
 *   - `category` uses the lenient serializer so a stale category string
 *     doesn't break the entire garage list decode (the field defaults to
 *     `null`, and the UI falls back to a neutral label).
 *   - `depositStatus` and `depositId` are populated only by the garage list
 *     endpoint; create/update responses leave them null.
 */
@Serializable
data class Equipment(
    val id: String,
    val name: String,
    @Serializable(with = NullableEquipmentCategorySerializer::class)
    val category: EquipmentCategory? = null,
    val characteristics: String = "",
    val additionalEquipment: String? = null,
    val photos: List<String> = emptyList(),
    val ownerId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val depositStatus: DepositStatus? = null,
    val depositId: String? = null,
) {
    /**
     * Returns a copy with the deposit fields rewritten. Used after a payment
     * completes so the cached row can flip from pending → paid without a
     * full re-fetch of the garage list.
     */
    fun withDeposit(status: DepositStatus?, id: String?): Equipment =
        copy(depositStatus = status, depositId = id)
}
