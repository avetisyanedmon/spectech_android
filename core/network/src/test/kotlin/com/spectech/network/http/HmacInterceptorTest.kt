package com.spectech.network.http

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.Test
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Self-consistent crypto checks for the HMAC signing scheme. These do NOT
 * require iOS to be running; they verify that the JDK crypto helpers behave
 * the way our interceptor assumes.
 *
 * The true iOS↔Android golden test (capture an iOS-signed request and assert
 * the Android signer produces the same hex) lives in `goldenIosSignature` —
 * fill in the captured inputs/outputs before enabling it.
 */
class HmacInterceptorTest {

    // SHA-256 -----------------------------------------------------------------

    /**
     * The backend hashes an empty payload for non-JSON request bodies. The
     * known SHA-256 of an empty byte string is fixed by the standard.
     */
    @Test fun `sha256 of empty body is the well-known FIPS constant`() {
        HmacInterceptor.sha256Hex(byteArrayOf()) shouldBe
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    }

    @Test fun `sha256 of 'abc' matches the FIPS-180 test vector`() {
        HmacInterceptor.sha256Hex("abc".toByteArray(Charsets.UTF_8)) shouldBe
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    }

    // HMAC --------------------------------------------------------------------

    @Test fun `HMAC-SHA256 is deterministic for the same inputs`() {
        val a = hmac("secret-key", "payload bytes")
        val b = hmac("secret-key", "payload bytes")
        a shouldBe b
        a shouldHaveLength 64
        a shouldMatch Regex("^[0-9a-f]+$")
    }

    @Test fun `HMAC-SHA256 changes when the payload changes`() {
        hmac("secret-key", "payload A") shouldNotBe hmac("secret-key", "payload B")
    }

    @Test fun `HMAC-SHA256 changes when the key changes`() {
        hmac("secret-1", "payload") shouldNotBe hmac("secret-2", "payload")
    }

    // Nonce + payload assembly ------------------------------------------------

    @Test fun `nonce is 32 lowercase hex chars`() {
        val nonce = HmacInterceptor.randomHex(16)
        nonce shouldHaveLength 32
        nonce shouldMatch Regex("^[0-9a-f]+$")
    }

    @Test fun `two consecutive nonces are different`() {
        HmacInterceptor.randomHex(16) shouldNotBe HmacInterceptor.randomHex(16)
    }

    @Test fun `payload string assembles in the exact iOS layout`() {
        val method = "POST"
        val pathAndQuery = "/api/auth/send-otp"
        val timestamp = "1700000000000"
        val nonce = "0123456789abcdef0123456789abcdef"
        val bodyHash = "abc"
        val actual = "$method\n$pathAndQuery\n$timestamp\n$nonce\n$bodyHash"
        actual shouldBe "POST\n/api/auth/send-otp\n1700000000000\n0123456789abcdef0123456789abcdef\nabc"
    }

    // Golden iOS comparison (manual setup) -----------------------------------

    /**
     * Disabled until paired with a captured iOS signature. To enable:
     *   1. Run the iOS app with a logging proxy (Charles/mitmproxy).
     *   2. Capture any signed request — note METHOD, the exact percent-encoded
     *      path-with-query, the X-Timestamp, X-Nonce, the raw body, and the
     *      X-Signature value.
     *   3. Paste those into the variables below and remove `@org.junit.jupiter.api.Disabled`.
     *
     * If this test passes after that, the Android signer is byte-for-byte
     * compatible with iOS.
     */
    @org.junit.jupiter.api.Disabled("Fill in captured iOS inputs to enable")
    @Test fun `golden iOS signature reproduces on Android`() {
        val secret = "66ff056ee8fa15b144a54ab472222b0a7534fe16286d1cfb893f6495fe65be96"
        val method = "POST"
        val pathAndQuery = "/api/auth/send-otp"     // ← capture from iOS
        val timestamp = "0000000000000"              // ← capture from iOS
        val nonce = "00000000000000000000000000000000" // ← capture from iOS
        val bodyJson = """{"phone":"+79991234567"}""" // ← capture from iOS
        val expectedSignature = "TODO"                // ← X-Signature from iOS

        val bodyHash = HmacInterceptor.sha256Hex(bodyJson.toByteArray(Charsets.UTF_8))
        val payload = "$method\n$pathAndQuery\n$timestamp\n$nonce\n$bodyHash"
        val sig = hmac(secret, payload)
        sig shouldBe expectedSignature
    }

    private fun hmac(key: String, message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(message.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
