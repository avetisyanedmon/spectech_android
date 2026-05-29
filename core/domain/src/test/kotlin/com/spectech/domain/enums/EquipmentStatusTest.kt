package com.spectech.domain.enums

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Tests for [EquipmentStatus.from] — the lenient parser introduced in Section
 * 7. The garage card and detail screen both depend on it to render the
 * status pill; if the parser drops a known spelling, the badge silently
 * disappears from the UI.
 *
 * The iOS source emits "Available" / "In Use" / "Maintenance" verbatim; we
 * additionally accept the legacy "Busy" alias (treated as in-use) and
 * tolerate snake_case, whitespace, and mixed-case input so backend records
 * written by older clients still classify correctly.
 */
class EquipmentStatusTest {

    // ─── canonical iOS spellings ────────────────────────────────────────

    @Test fun `Available maps to AVAILABLE`() {
        EquipmentStatus.from("Available") shouldBe EquipmentStatus.AVAILABLE
    }

    @Test fun `In Use maps to IN_USE`() {
        EquipmentStatus.from("In Use") shouldBe EquipmentStatus.IN_USE
    }

    @Test fun `Maintenance maps to MAINTENANCE`() {
        EquipmentStatus.from("Maintenance") shouldBe EquipmentStatus.MAINTENANCE
    }

    // ─── case insensitivity ─────────────────────────────────────────────

    @Test fun `lowercase available is recognised`() {
        EquipmentStatus.from("available") shouldBe EquipmentStatus.AVAILABLE
    }

    @Test fun `uppercase MAINTENANCE is recognised`() {
        EquipmentStatus.from("MAINTENANCE") shouldBe EquipmentStatus.MAINTENANCE
    }

    @Test fun `mixed-case In USE is recognised`() {
        EquipmentStatus.from("In USE") shouldBe EquipmentStatus.IN_USE
    }

    // ─── whitespace tolerance ───────────────────────────────────────────

    @Test fun `surrounding whitespace is trimmed`() {
        EquipmentStatus.from("   Available   ") shouldBe EquipmentStatus.AVAILABLE
    }

    @Test fun `tab-separated whitespace is trimmed`() {
        EquipmentStatus.from("\tMaintenance\n") shouldBe EquipmentStatus.MAINTENANCE
    }

    // ─── snake_case alias ───────────────────────────────────────────────

    @Test fun `in_use snake_case alias maps to IN_USE`() {
        EquipmentStatus.from("in_use") shouldBe EquipmentStatus.IN_USE
    }

    // ─── legacy Busy alias ──────────────────────────────────────────────

    @Test fun `Busy legacy alias maps to IN_USE`() {
        EquipmentStatus.from("Busy") shouldBe EquipmentStatus.IN_USE
    }

    @Test fun `busy lowercase legacy alias maps to IN_USE`() {
        EquipmentStatus.from("busy") shouldBe EquipmentStatus.IN_USE
    }

    // ─── null / empty / unknown ─────────────────────────────────────────

    @Test fun `null returns null`() {
        EquipmentStatus.from(null) shouldBe null
    }

    @Test fun `empty string returns null`() {
        EquipmentStatus.from("") shouldBe null
    }

    @Test fun `whitespace-only string returns null`() {
        EquipmentStatus.from("   ") shouldBe null
    }

    @Test fun `unknown value returns null instead of throwing`() {
        EquipmentStatus.from("Decommissioned") shouldBe null
        EquipmentStatus.from("Active") shouldBe null
        EquipmentStatus.from("Idle") shouldBe null
    }

    @Test fun `numeric noise returns null`() {
        EquipmentStatus.from("42") shouldBe null
    }
}
