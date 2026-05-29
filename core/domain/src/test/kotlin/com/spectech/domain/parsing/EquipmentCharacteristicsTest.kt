package com.spectech.domain.parsing

import com.spectech.domain.enums.EquipmentStatus
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Tests for [EquipmentCharacteristics] — the " | "-separated "Key: Value"
 * parser ported from iOS in Section 7. The Garage card, equipment detail
 * screen, and the bid card all depend on it; a broken parser silently
 * blanks the VIN / City / Status / Year / Description fields in every
 * affected row.
 *
 * iOS rules being asserted:
 *   - Split on the literal " | " (space-pipe-space). A bare "|" doesn't count.
 *   - Each part is "Key: Value" with the literal ": " (colon-space). A bare
 *     ":" doesn't count.
 *   - Key match is case-SENSITIVE after a trim. "vin" doesn't find "VIN".
 *   - Value is trimmed; empty values resolve to null.
 *   - Values containing ": " are preserved (the parser only splits on the
 *     FIRST colon).
 *   - Missing keys return null. Unparseable input returns null per-field.
 */
class EquipmentCharacteristicsTest {

    // ─── basic field extraction ────────────────────────────────────────

    @Test fun `extracts VIN from canonical payload`() {
        val raw = "VIN: ABC123XYZ | City: Moscow | Status: Available"
        EquipmentCharacteristics.vin(raw) shouldBe "ABC123XYZ"
    }

    @Test fun `extracts City from canonical payload`() {
        val raw = "VIN: ABC123 | City: Moscow | Status: Available"
        EquipmentCharacteristics.city(raw) shouldBe "Moscow"
    }

    @Test fun `extracts Year from canonical payload`() {
        val raw = "VIN: ABC123 | Year: 2019 | Description: Reliable unit"
        EquipmentCharacteristics.year(raw) shouldBe "2019"
    }

    @Test fun `extracts Description from canonical payload`() {
        val raw = "VIN: ABC123 | Year: 2019 | Description: Reliable unit"
        EquipmentCharacteristics.description(raw) shouldBe "Reliable unit"
    }

    // ─── status parsing through EquipmentStatus.from ───────────────────

    @Test fun `status() resolves Available to AVAILABLE`() {
        val raw = "VIN: 1 | Status: Available"
        EquipmentCharacteristics.status(raw) shouldBe EquipmentStatus.AVAILABLE
    }

    @Test fun `status() resolves In Use to IN_USE`() {
        val raw = "VIN: 1 | Status: In Use"
        EquipmentCharacteristics.status(raw) shouldBe EquipmentStatus.IN_USE
    }

    @Test fun `status() resolves Maintenance to MAINTENANCE`() {
        val raw = "VIN: 1 | Status: Maintenance"
        EquipmentCharacteristics.status(raw) shouldBe EquipmentStatus.MAINTENANCE
    }

    @Test fun `status() returns null for unknown status value`() {
        val raw = "VIN: 1 | Status: Decommissioned"
        EquipmentCharacteristics.status(raw) shouldBe null
    }

    // ─── absent / blank input ──────────────────────────────────────────

    @Test fun `null raw returns null for every field`() {
        EquipmentCharacteristics.vin(null) shouldBe null
        EquipmentCharacteristics.city(null) shouldBe null
        EquipmentCharacteristics.year(null) shouldBe null
        EquipmentCharacteristics.status(null) shouldBe null
        EquipmentCharacteristics.description(null) shouldBe null
    }

    @Test fun `empty raw returns null for every field`() {
        EquipmentCharacteristics.vin("") shouldBe null
        EquipmentCharacteristics.city("") shouldBe null
    }

    @Test fun `whitespace-only raw returns null for every field`() {
        EquipmentCharacteristics.vin("   ") shouldBe null
        EquipmentCharacteristics.city("\t\n") shouldBe null
    }

    @Test fun `missing key returns null even when other keys are present`() {
        val raw = "VIN: ABC123 | City: Moscow"
        EquipmentCharacteristics.status(raw) shouldBe null
        EquipmentCharacteristics.year(raw) shouldBe null
        EquipmentCharacteristics.description(raw) shouldBe null
    }

    // ─── case sensitivity (iOS contract) ───────────────────────────────

    @Test fun `key match is case-sensitive — lowercase vin does not resolve VIN`() {
        // iOS uses `==` after a trim for the key comparison; we keep parity
        // so a backend record written by the Android form (which always
        // writes "VIN") doesn't accidentally match an iOS-written "vin".
        val raw = "VIN: ABC123 | City: Moscow"
        EquipmentCharacteristics.field(raw, "vin") shouldBe null
        EquipmentCharacteristics.field(raw, "VIN") shouldBe "ABC123"
    }

    // ─── value trimming ────────────────────────────────────────────────

    @Test fun `value is trimmed of surrounding whitespace`() {
        val raw = "VIN:    ABC123    | City: Moscow"
        // The form writes "Key: Value" with a single space; tolerate extra
        // padding the same way iOS does via `.trimmingCharacters(in: .whitespaces)`.
        EquipmentCharacteristics.vin(raw) shouldBe "ABC123"
    }

    @Test fun `empty value resolves to null even when key is present`() {
        val raw = "VIN:  | City: Moscow"
        EquipmentCharacteristics.vin(raw) shouldBe null
        EquipmentCharacteristics.city(raw) shouldBe "Moscow"
    }

    // ─── colons inside values ──────────────────────────────────────────

    @Test fun `value containing colon-space is preserved verbatim`() {
        // The substring(sepIdx + 2) split takes the FIRST ": " only — any
        // additional `": "` characters belong to the value. This matters for
        // descriptions that read like "Note: needs servicing".
        val raw = "VIN: ABC | Description: Note: needs servicing"
        EquipmentCharacteristics.description(raw) shouldBe "Note: needs servicing"
    }

    // ─── separators ────────────────────────────────────────────────────

    @Test fun `parts separated by bare pipe without spaces are not split`() {
        // iOS uses ` | ` (literal space-pipe-space) as the separator. A
        // "VIN: 1|City: Moscow" string is treated as a single garbled VIN
        // value rather than two fields.
        val raw = "VIN: 1|City: Moscow"
        EquipmentCharacteristics.vin(raw) shouldBe "1|City: Moscow"
        EquipmentCharacteristics.city(raw) shouldBe null
    }

    @Test fun `key without colon-space pair is ignored`() {
        // A part like "VIN ABC123" (missing ": " delimiter) is not extracted.
        val raw = "VIN ABC123 | City: Moscow"
        EquipmentCharacteristics.vin(raw) shouldBe null
        EquipmentCharacteristics.city(raw) shouldBe "Moscow"
    }

    // ─── repeated keys (last definition wins is NOT the rule) ──────────

    @Test fun `first occurrence of a key wins when duplicated`() {
        // iOS returns from the loop on first match. Don't change this to
        // "last wins" without a corresponding iOS change.
        val raw = "VIN: FIRST | VIN: SECOND | City: Moscow"
        EquipmentCharacteristics.vin(raw) shouldBe "FIRST"
    }

    // ─── round-trip with the form's writer shape ───────────────────────

    @Test fun `round-trip parses the Add Equipment form's typical write shape`() {
        // Add Equipment writes "VIN: <vin> | Year: <year> | Description: <desc>"
        // (and optionally Status / City via Edit). Confirm the parser handles
        // the four canonical fields together in one payload.
        val raw = "VIN: 4Y1SL65848Z411439 | City: Saint Petersburg | Year: 2018 | Description: Owner-driven excavator"
        EquipmentCharacteristics.vin(raw) shouldBe "4Y1SL65848Z411439"
        EquipmentCharacteristics.city(raw) shouldBe "Saint Petersburg"
        EquipmentCharacteristics.year(raw) shouldBe "2018"
        EquipmentCharacteristics.description(raw) shouldBe "Owner-driven excavator"
    }
}
