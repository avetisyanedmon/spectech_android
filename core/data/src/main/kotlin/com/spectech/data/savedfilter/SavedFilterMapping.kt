package com.spectech.data.savedfilter

import com.spectech.domain.enums.EquipmentCategory
import com.spectech.domain.enums.PaymentType
import com.spectech.domain.enums.PricingUnit
import com.spectech.domain.model.OrderFilters
import com.spectech.network.endpoints.SavedFilterPayload

/**
 * Mapping between the typed [OrderFilters] used in UI and the wire-shape
 * [SavedFilterPayload] the backend expects. Mirrors iOS
 * `OrderFilters.serverPayload` / `init(payload:)` and sorts every array so a
 * round-trip is byte-stable — that lets the server skip writes when nothing
 * actually changed.
 *
 * Codes that the local enum can't resolve (server added a new category we
 * don't know about) are dropped silently — the user keeps their other
 * picks; an out-of-date client never erases unknown server state because
 * we never blind-PUT what we receive without going through the user.
 */
fun OrderFilters.toPayload(): SavedFilterPayload = SavedFilterPayload(
    categories = categories.map { it.wire }.sorted(),
    regions = regions.sorted(),
    cities = selectedCities.sorted(),
    pricingUnits = pricingUnits.map { it.wire }.sorted(),
    paymentTypes = paymentTypes.map { it.wire }.sorted(),
)

fun SavedFilterPayload.toOrderFilters(): OrderFilters = OrderFilters(
    categories = categories
        .mapNotNull { code -> EquipmentCategory.entries.firstOrNull { it.wire == code } }
        .toSet(),
    regions = regions.toSet(),
    selectedCities = cities.toSet(),
    pricingUnits = pricingUnits
        .mapNotNull { code -> PricingUnit.entries.firstOrNull { it.wire == code } }
        .toSet(),
    paymentTypes = paymentTypes
        .mapNotNull { code -> PaymentType.entries.firstOrNull { it.wire == code } }
        .toSet(),
)
