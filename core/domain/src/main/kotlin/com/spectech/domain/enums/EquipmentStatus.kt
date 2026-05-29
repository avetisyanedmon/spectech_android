package com.spectech.domain.enums

/**
 * Operational status of a contractor's equipment unit, embedded as
 * `"Status: <value>"` inside [com.spectech.domain.model.Equipment.characteristics].
 * Mirrors iOS `EquipmentStatus` (SpecTechIOS/Scene/Tabs/Garage/Model/EquipmentStatus.swift)
 * — same three canonical values, same wire strings.
 *
 * Not part of the wire envelope — it lives inside the free-text characteristics
 * blob the Garage / Add-Equipment views write. The lenient [from] companion
 * accepts the legacy "Busy" alias for "in use" so older records still render
 * with the right colour.
 */
enum class EquipmentStatus(val titleKey: String) {
    /** Listed and available for bidding. Green pill. */
    AVAILABLE("equipment_status_available"),

    /** Currently rented out / actively working a job. Red pill. */
    IN_USE("equipment_status_in_use"),

    /** Out of service for repairs. Amber pill. */
    MAINTENANCE("equipment_status_maintenance");

    companion object {
        /**
         * Tolerates every spelling iOS' [String(localized:)] roundtrip can
         * produce: canonical "Available" / "In Use" / "Maintenance", legacy
         * "Busy" (treated as in-use), and any case / whitespace combination.
         * Returns `null` for an unknown value so the UI can render a neutral
         * placeholder instead of crashing.
         */
        fun from(raw: String?): EquipmentStatus? {
            val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return when (trimmed.lowercase()) {
                "available" -> AVAILABLE
                "in use", "in_use", "busy" -> IN_USE
                "maintenance" -> MAINTENANCE
                else -> null
            }
        }
    }
}
