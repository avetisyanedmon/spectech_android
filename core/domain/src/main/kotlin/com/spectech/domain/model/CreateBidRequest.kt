package com.spectech.domain.model

import com.spectech.domain.serializers.BigDecimalSerializer
import java.math.BigDecimal
import kotlinx.serialization.Serializable

/**
 * Wire shape for `POST /orders/{orderId}/bids`. Mirrors iOS `CreateBidRequest`
 * (SpecTechIOS/Shared/Models/DomainModels.swift).
 *
 *   - `paymentType` and `equipmentCategory` are sent as the Russian display
 *     strings the backend expects in create payloads.
 *   - `equipmentPhotos` are the hosted Supabase URLs already returned by
 *     `/equipment/photos/upload` — never base64.
 *   - Contractor identity is included flat (phone + name); the backend uses
 *     it to populate the order's `creatorPhone`/`creatorName` once the
 *     customer accepts.
 */
@Serializable
data class CreateBidRequest(
    @Serializable(with = BigDecimalSerializer::class) val price: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val deliveryPrice: BigDecimal,
    val paymentType: String,
    val comment: String,
    val equipmentId: String,
    val equipmentName: String,
    val equipmentCategory: String,
    val equipmentPhotos: List<String>,
    val equipmentCharacteristics: String,
    val equipmentAdditionalInfo: String,
    val contractorPhone: String,
    val contractorName: String,
)
