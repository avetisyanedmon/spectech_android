package com.spectech.domain.model

import kotlinx.serialization.Serializable

/**
 * Wire shape for `POST /orders`. Mirrors iOS `CreateOrderRequest`
 * (SpecTechIOS/Shared/Models/DomainModels.swift) — every value is already
 * stringified the way the backend expects, including the Russian display
 * strings for `equipmentCategory`, `paymentTypes`, and `pricingUnit`.
 *
 * The VM builds this from raw form state; the network layer just serialises
 * it (see [com.spectech.network.endpoints.OrdersApi.CreateOrder]).
 */
@Serializable
data class CreateOrderRequest(
    val equipmentCategory: String,    // Russian display, e.g. "Самосвал"
    val city: String,
    val street: String,
    val houseNumber: String,
    val address: String,              // "region, street, house" joined
    val paymentTypes: List<String>,   // Russian display values
    val pricingUnit: String,          // Russian display value
    val workVolume: Double,
    val startDate: String,            // "yyyy-MM-dd"
    val startTime: String,            // "HH:mm:ss"
    val startDateTime: String,        // ISO-8601
    val adDuration: Int,              // seconds until biddingDeadline, >= 60
    val durationHours: Int,
    val expiryDateTime: String,       // ISO-8601 (the biddingDeadline)
    val description: String? = null,
)
