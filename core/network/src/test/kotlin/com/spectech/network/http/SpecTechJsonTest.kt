package com.spectech.network.http

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Test

/**
 * Locks the [SpecTechJson] wire contract: keys are passed through verbatim,
 * camelCase end-to-end, matching the spectech_backoffice payloads and the iOS
 * `JSONEncoder` default-strategy behaviour.
 *
 * This regression guard exists because the Garage tab silently broke when
 * `JsonNamingStrategy.SnakeCase` was added to the global config — multi-word
 * required fields like `Equipment.ownerId` / `createdAt` / `updatedAt` failed
 * to decode because kotlinx was looking for `owner_id` / `created_at` while
 * the wire shipped camelCase. If a future commit re-introduces the strategy
 * (or any other key transformation) these tests will fail fast.
 */
class SpecTechJsonTest {

    @Serializable
    private data class WireSample(
        val id: String,
        val ownerId: String,
        val createdAt: String,
        val additionalEquipment: String? = null,
        val accountType: String? = null,
        val isAccepted: Boolean = false,
    )

    @Test fun `multi-word camelCase keys decode verbatim from the wire`() {
        // This is the exact shape spectech_backoffice ships from GET /equipment
        // and from /auth/verify-otp. The decoder MUST match camelCase wire keys
        // against camelCase Kotlin property names without any transformation.
        val json = """
            {
              "id": "eq-1",
              "ownerId": "user-42",
              "createdAt": "2026-05-29T10:00:00Z",
              "additionalEquipment": "bucket",
              "accountType": "individual",
              "isAccepted": true
            }
        """.trimIndent()

        val decoded = SpecTechJson.decodeFromString<WireSample>(json)

        decoded.id shouldBe "eq-1"
        decoded.ownerId shouldBe "user-42"
        decoded.createdAt shouldBe "2026-05-29T10:00:00Z"
        decoded.additionalEquipment shouldBe "bucket"
        decoded.accountType shouldBe "individual"
        decoded.isAccepted shouldBe true
    }

    @Test fun `multi-word camelCase keys encode verbatim onto the wire`() {
        // Request bodies — the backend validators (auth, equipment, orders,
        // bids, deposits) require camelCase keys. iOS encodes with the default
        // strategy and emits the Swift property names; Android must too.
        val sample = WireSample(
            id = "eq-1",
            ownerId = "user-42",
            createdAt = "2026-05-29T10:00:00Z",
            additionalEquipment = "bucket",
            accountType = "individual",
            isAccepted = true,
        )

        val encoded = SpecTechJson.encodeToString(sample)

        encoded shouldContain "\"ownerId\":\"user-42\""
        encoded shouldContain "\"createdAt\":\"2026-05-29T10:00:00Z\""
        encoded shouldContain "\"additionalEquipment\":\"bucket\""
        encoded shouldContain "\"accountType\":\"individual\""
        encoded shouldContain "\"isAccepted\":true"

        // Guard against a future regression where a naming strategy gets
        // re-added and silently transforms keys to snake_case.
        encoded shouldNotContain "owner_id"
        encoded shouldNotContain "created_at"
        encoded shouldNotContain "additional_equipment"
        encoded shouldNotContain "account_type"
        encoded shouldNotContain "is_accepted"
    }

    @Test fun `unknown wire fields are tolerated`() {
        // Backend may add fields between releases; older clients must keep
        // decoding successfully.
        val json = """
            {
              "id": "eq-1",
              "ownerId": "user-42",
              "createdAt": "2026-05-29T10:00:00Z",
              "futureField": "value"
            }
        """.trimIndent()

        val decoded = SpecTechJson.decodeFromString<WireSample>(json)
        decoded.id shouldBe "eq-1"
    }

    @Test fun `default values are omitted on encode`() {
        // Matches iOS JSONEncoder default behaviour — `null` fields disappear
        // from the wire payload and defaults aren't sent unless explicitly set.
        val sample = WireSample(
            id = "eq-1",
            ownerId = "user-42",
            createdAt = "2026-05-29T10:00:00Z",
        )

        val encoded = SpecTechJson.encodeToString(sample)

        encoded shouldNotContain "additionalEquipment"
        encoded shouldNotContain "accountType"
        encoded shouldNotContain "isAccepted"
    }
}
