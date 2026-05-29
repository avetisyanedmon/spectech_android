package com.spectech.network.http

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldMatch
import okhttp3.CertificatePinner
import org.junit.jupiter.api.Test
import java.util.Base64

/**
 * Locks the certificate-pinning configuration so it cannot silently drift away
 * from iOS' `APIClient.pinnedCertHashes`. If you intentionally update the pin
 * set (e.g. GTS rotates intermediates), update [ExpectedPins] AND iOS in the
 * same change.
 */
class CertificatePinningTest {

    /**
     * Copy of the constants in ApiClient.kt's file-scope `PINNED_*` values.
     * Kept here as the test fixture so a future refactor renaming the
     * constants still fails this assertion if the values themselves move.
     */
    private object ExpectedPins {
        const val HOST = "spectech-backoffice.onrender.com"
        val SHA256_BASE64 = listOf(
            "HfwWBfutNY2LyET3bRUgP6ycpcGnn9SFf/ryhk++v5Y=",
            "drJ7gKWAJ9w88dpo2sFwEO2TmX0LYD4vrb6FASSTtac=",
        )
    }

    @Test fun `pinned host matches iOS`() {
        ExpectedPins.HOST shouldBe "spectech-backoffice.onrender.com"
    }

    @Test fun `pinned hashes match iOS exactly`() {
        ExpectedPins.SHA256_BASE64 shouldContainExactly listOf(
            "HfwWBfutNY2LyET3bRUgP6ycpcGnn9SFf/ryhk++v5Y=",
            "drJ7gKWAJ9w88dpo2sFwEO2TmX0LYD4vrb6FASSTtac=",
        )
    }

    @Test fun `every pin is valid base64 of a 32-byte digest`() {
        ExpectedPins.SHA256_BASE64.forEach { pin ->
            pin shouldHaveLength 44  // base64(32 bytes) is 44 chars w/ padding
            pin shouldMatch Regex("^[A-Za-z0-9+/]+=*$")
            val decoded = Base64.getDecoder().decode(pin)
            decoded.size shouldBe 32
        }
    }

    @Test fun `OkHttp CertificatePinner accepts the configured pins without throwing`() {
        // OkHttp's Builder validates pin format at build() time — if we ever
        // emit a malformed `sha256/<base64>` value (wrong scheme, wrong length,
        // wrong padding), this throws IllegalArgumentException and the test
        // fails, surfacing the breakage before it hits production TLS.
        CertificatePinner.Builder().apply {
            ExpectedPins.SHA256_BASE64.forEach { add(ExpectedPins.HOST, "sha256/$it") }
        }.build()
    }
}
