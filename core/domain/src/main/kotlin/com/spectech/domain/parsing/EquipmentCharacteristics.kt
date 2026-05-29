package com.spectech.domain.parsing

import com.spectech.domain.enums.EquipmentStatus

/**
 * Parses the " | "-separated, "Key: Value" payload that the Garage feature
 * writes into [com.spectech.domain.model.Equipment.characteristics]. Mirrors
 * iOS `parseCharacteristic`
 * (SpecTechIOS/Scene/Tabs/Garage/Equipment/EquipmentDetailView.swift:19-27
 * and EquipmentCardView.swift:6-14) byte-for-byte.
 *
 * Keys used by the iOS form (kept here as constants so callers don't need to
 * remember the exact casing):
 *   - "VIN"     — vehicle identification number
 *   - "City"    — owner-provided city for the unit
 *   - "Status"  — `EquipmentStatus` text ("Available" / "In Use" / "Maintenance")
 *   - "Year"    — manufacturing year (read by the bid card today; see Section 4)
 *   - "Description" — free-form description (also read by the bid card)
 *
 * Lives in `core/domain/parsing/` so the parsing layer can be unit-tested
 * without pulling in Android/Compose dependencies. Originally introduced in
 * the ui-kit module during Section 7, hoisted to domain in Section 11 for
 * proper test coverage.
 */
object EquipmentCharacteristics {

    const val KEY_VIN = "VIN"
    const val KEY_CITY = "City"
    const val KEY_STATUS = "Status"
    const val KEY_YEAR = "Year"
    const val KEY_DESCRIPTION = "Description"

    /**
     * Returns the value associated with [key] (exact case-sensitive match,
     * trimmed) or `null` if the key is missing / the value is empty. iOS
     * matches with a case-sensitive `==` on the trimmed first token.
     */
    fun field(raw: String?, key: String): String? {
        val payload = raw?.takeIf { it.isNotBlank() } ?: return null
        for (part in payload.split(" | ")) {
            val sepIdx = part.indexOf(": ")
            if (sepIdx <= 0) continue
            val name = part.substring(0, sepIdx).trim()
            if (name == key) {
                val value = part.substring(sepIdx + 2).trim()
                return value.takeIf { it.isNotEmpty() }
            }
        }
        return null
    }

    fun vin(raw: String?): String? = field(raw, KEY_VIN)
    fun city(raw: String?): String? = field(raw, KEY_CITY)
    fun year(raw: String?): String? = field(raw, KEY_YEAR)
    fun description(raw: String?): String? = field(raw, KEY_DESCRIPTION)

    /** Returns the parsed [EquipmentStatus] enum, or `null` for unknown values. */
    fun status(raw: String?): EquipmentStatus? = EquipmentStatus.from(field(raw, KEY_STATUS))
}
