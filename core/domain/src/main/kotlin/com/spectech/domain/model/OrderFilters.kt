package com.spectech.domain.model

import com.spectech.domain.enums.EquipmentCategory
import com.spectech.domain.enums.PaymentType
import com.spectech.domain.enums.PricingUnit
import kotlinx.serialization.Serializable

/**
 * Mirrors iOS `OrderFilters` (SpecTechIOS/Shared/Models/DomainModels.swift)
 * including its `matches(order:)` semantics — bug-compatible:
 *   - Non-empty category filter rejects orders with a null category.
 *   - Region match runs a case-insensitive substring on `order.address`,
 *     because CreateOrder serialises the region into the address string
 *     ("region, street, house").
 *   - Empty filter sets mean "no restriction" for that dimension.
 */
@Serializable
data class OrderFilters(
    val categories: Set<EquipmentCategory> = emptySet(),
    val regions: Set<String> = emptySet(),
    val selectedCities: Set<String> = emptySet(),
    val pricingUnits: Set<PricingUnit> = emptySet(),
    val paymentTypes: Set<PaymentType> = emptySet(),
) {
    val isEmpty: Boolean
        get() = categories.isEmpty() &&
                regions.isEmpty() &&
                selectedCities.isEmpty() &&
                pricingUnits.isEmpty() &&
                paymentTypes.isEmpty()

    fun matches(order: Order): Boolean {
        if (categories.isNotEmpty()) {
            val cat = order.equipmentCategory ?: return false
            if (cat !in categories) return false
        }
        if (selectedCities.isNotEmpty() &&
            selectedCities.none { it.lowercase() in order.city.lowercase() }) {
            return false
        }
        if (regions.isNotEmpty()) {
            val haystack = order.address.orEmpty().lowercase()
            if (regions.none { it.lowercase() in haystack }) return false
        }
        if (pricingUnits.isNotEmpty()) {
            val unit = order.pricingUnit ?: return false
            if (unit !in pricingUnits) return false
        }
        if (paymentTypes.isNotEmpty()) {
            if (order.paymentTypes.toSet().intersect(paymentTypes).isEmpty()) return false
        }
        return true
    }
}
