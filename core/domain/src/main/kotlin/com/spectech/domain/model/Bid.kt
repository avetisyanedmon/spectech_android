package com.spectech.domain.model

import com.spectech.domain.enums.EquipmentCategory
import com.spectech.domain.enums.PaymentType
import com.spectech.domain.serializers.BigDecimalSerializer
import com.spectech.domain.serializers.NullableEquipmentCategorySerializer
import com.spectech.domain.serializers.NullablePaymentTypeSerializer
import java.math.BigDecimal
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Mirrors iOS `Bid` (SpecTechIOS/Shared/Models/DomainModels.swift).
 *
 * Contractor identity can be present on the wire under any of `contractor`,
 * `user`, or `submittedBy`; flat `contractorPhone`/`contractorName` fields
 * exist on older responses. [contractorContact] resolves whichever is
 * actually populated — same precedence as iOS.
 */
@Serializable
data class Bid(
    val id: String,
    val userId: String? = null,
    val contractorId: String? = null,
    @Serializable(with = BigDecimalSerializer::class) val price: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val deliveryPrice: BigDecimal? = null,
    @Serializable(with = NullablePaymentTypeSerializer::class) val paymentType: PaymentType? = null,
    val comment: String? = null,
    val equipmentId: String? = null,
    val equipmentName: String? = null,
    @Serializable(with = NullableEquipmentCategorySerializer::class) val equipmentCategory: EquipmentCategory? = null,
    val equipmentPhotos: List<String>? = null,
    val equipmentCharacteristics: String? = null,
    val equipmentAdditionalInfo: String? = null,
    val isAccepted: Boolean = false,
    val submittedAt: Instant? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val contractorPhone: String? = null,
    val contractorName: String? = null,
    val contractor: ContractorInfo? = null,
    val user: ContractorInfo? = null,
    val submittedBy: ContractorInfo? = null,
) {
    val contractorContact: ContractorContact?
        get() {
            val nested = contractor ?: user ?: submittedBy
            val phone = nested?.phone ?: contractorPhone
            val name = nested?.name ?: contractorName
            return if (phone != null) ContractorContact(phone, name) else null
        }
}
