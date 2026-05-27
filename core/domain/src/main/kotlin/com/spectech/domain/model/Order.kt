package com.spectech.domain.model

import com.spectech.domain.enums.EquipmentCategory
import com.spectech.domain.enums.OrderStatus
import com.spectech.domain.enums.PaymentType
import com.spectech.domain.enums.PricingUnit
import com.spectech.domain.serializers.NullableEquipmentCategorySerializer
import com.spectech.domain.serializers.NullablePricingUnitSerializer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Mirrors iOS `Order` (SpecTechIOS/Shared/Models/DomainModels.swift).
 *
 *   - `equipmentCategory` is tolerant: unknown values decode to null (a stale
 *     category from an older backend version shouldn't fail the whole list).
 *   - `paymentTypes` defaults to empty so older responses without the field
 *     still parse.
 *   - `expiresAt` and `expiryDateTime` are both supported because the backend
 *     has shipped both field names at different times. See [effectiveExpiry].
 */
@Serializable
data class Order(
    val id: String,
    @Serializable(with = NullableEquipmentCategorySerializer::class) val equipmentCategory: EquipmentCategory? = null,
    val city: String,
    val address: String? = null,
    val description: String? = null,
    val paymentTypes: List<PaymentType> = emptyList(),
    @Serializable(with = NullablePricingUnitSerializer::class) val pricingUnit: PricingUnit? = null,
    val workVolume: Double? = null,
    val startDateTime: Instant? = null,
    val durationHours: Int? = null,
    val expiresAt: Instant? = null,
    val expiryDateTime: Instant? = null,
    val status: OrderStatus = OrderStatus.OPEN,
    val bidCount: Int = 0,
    val creatorId: String? = null,
    val creatorPhone: String? = null,
    val creatorName: String? = null,
    val createdAt: Instant? = null,
    val bids: List<Bid> = emptyList(),
    val acceptedBidId: String? = null,
) {
    val effectiveExpiry: Instant? get() = expiresAt ?: expiryDateTime

    val isExpired: Boolean
        get() = effectiveExpiry?.let { it < Clock.System.now() } == true

    /** Address-first display string, falling back to city when address is missing. */
    val displayAddress: String
        get() = address?.takeIf { it.isNotBlank() } ?: city

    /** `"city, address"` for places where both should be visible together. */
    val fullAddress: String
        get() {
            val a = address?.trim().orEmpty()
            val c = city.trim()
            return when {
                a.isEmpty() -> c
                c.isEmpty() -> a
                else -> "$c, $a"
            }
        }
}
