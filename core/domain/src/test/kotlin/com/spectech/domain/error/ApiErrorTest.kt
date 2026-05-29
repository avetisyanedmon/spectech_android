package com.spectech.domain.error

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Test

/**
 * Tests for [ApiError.from] / [ApiError.fallback], the classification
 * framework introduced in Section 2 of the iOS-parity work. Each typed
 * exception the network layer (Ktor / OkHttp / kotlinx.serialization) is
 * known to throw must map to a stable [ApiError.LocalCodes] entry so the
 * `core/ui-kit` `localizedMessage()` extension can resolve the user-facing
 * localized string.
 *
 * If a future commit breaks this mapping, every error banner across the app
 * silently falls back to the generic "Something went wrong" copy — these
 * tests are the contract.
 */
class ApiErrorTest {

    // ─── exception → code classification ───────────────────────────────

    @Test fun `passes through an existing ApiError unchanged`() {
        val original = ApiError(
            statusCode = 403,
            code = ApiError.LocalCodes.FALLBACK_403,
            message = "You can't see that.",
        )
        val classified = ApiError.from(original)
        classified shouldBe original
    }

    @Test fun `UnknownHostException maps to NETWORK_OFFLINE`() {
        val result = ApiError.from(UnknownHostException("api.spectech.test"))
        result.code shouldBe ApiError.LocalCodes.NETWORK_OFFLINE
    }

    @Test fun `ConnectException maps to NETWORK_OFFLINE`() {
        val result = ApiError.from(ConnectException("connection refused"))
        result.code shouldBe ApiError.LocalCodes.NETWORK_OFFLINE
    }

    @Test fun `NoRouteToHostException maps to NETWORK_OFFLINE`() {
        val result = ApiError.from(NoRouteToHostException("no route"))
        result.code shouldBe ApiError.LocalCodes.NETWORK_OFFLINE
    }

    @Test fun `Plain IOException without a recognised subclass maps to NETWORK_OFFLINE`() {
        // The repository's catch block hits this when OkHttp returns a generic
        // IOException for transient network failures the classifier doesn't
        // recognise specifically.
        val result = ApiError.from(IOException("stream closed"))
        result.code shouldBe ApiError.LocalCodes.NETWORK_OFFLINE
    }

    @Test fun `SocketTimeoutException maps to SERVER_TIMEOUT`() {
        val result = ApiError.from(SocketTimeoutException("read timed out"))
        result.code shouldBe ApiError.LocalCodes.SERVER_TIMEOUT
    }

    @Test fun `SSLHandshakeException maps to SSL_FAILURE`() {
        val result = ApiError.from(SSLHandshakeException("untrusted issuer"))
        result.code shouldBe ApiError.LocalCodes.SSL_FAILURE
    }

    @Test fun `SSLPeerUnverifiedException maps to SSL_FAILURE`() {
        // Thrown by OkHttp's CertificatePinner when the served chain doesn't
        // match the pinned cert hashes — the most likely real-world hit when
        // the GTS intermediates rotate.
        val result = ApiError.from(SSLPeerUnverifiedException("pin mismatch"))
        result.code shouldBe ApiError.LocalCodes.SSL_FAILURE
    }

    @Test fun `SSL exception wrapped inside IOException is still classified as SSL_FAILURE`() {
        // Ktor / OkHttp wrap the underlying cert error inside an IOException
        // on certain paths — the cause-chain walk in ApiError.from must look
        // past the IOException to the SSLPeerUnverifiedException it wraps.
        val wrapped = IOException("transport failed", SSLPeerUnverifiedException("pin mismatch"))
        val result = ApiError.from(wrapped)
        result.code shouldBe ApiError.LocalCodes.SSL_FAILURE
    }

    @Test fun `kotlinx serialization exception maps to DECODING_FAILED`() {
        val result = ApiError.from(SerializationException("missing field 'id'"))
        result.code shouldBe ApiError.LocalCodes.DECODING_FAILED
    }

    @Test fun `Unknown RuntimeException falls back to GENERIC_UNKNOWN`() {
        val result = ApiError.from(RuntimeException("the universe is broken"))
        result.code shouldBe ApiError.LocalCodes.GENERIC_UNKNOWN
    }

    // ─── status-code fallbacks ────────────────────────────────────────

    @Test fun `fallback(400) carries the FALLBACK_400 code`() {
        val result = ApiError.fallback(400)
        result.statusCode shouldBe 400
        result.code shouldBe ApiError.LocalCodes.FALLBACK_400
    }

    @Test fun `fallback(401) is the session-expired sentinel`() {
        val result = ApiError.fallback(401)
        result.statusCode shouldBe 401
        result.code shouldBe ApiError.LocalCodes.FALLBACK_401
        result.isUnauthorized shouldBe true
    }

    @Test fun `fallback(403) has permission-denied code`() {
        val result = ApiError.fallback(403)
        result.code shouldBe ApiError.LocalCodes.FALLBACK_403
    }

    @Test fun `fallback(404) has not-found code`() {
        val result = ApiError.fallback(404)
        result.code shouldBe ApiError.LocalCodes.FALLBACK_404
    }

    @Test fun `fallback(429) has rate-limit code`() {
        val result = ApiError.fallback(429)
        result.code shouldBe ApiError.LocalCodes.FALLBACK_429
    }

    @Test fun `fallback(503) has server-unavailable code`() {
        val result = ApiError.fallback(503)
        result.code shouldBe ApiError.LocalCodes.FALLBACK_503
    }

    @Test fun `fallback(500) and other unknown codes use FALLBACK_OTHER`() {
        val result = ApiError.fallback(500)
        result.statusCode shouldBe 500
        result.code shouldBe ApiError.LocalCodes.FALLBACK_OTHER
    }

    @Test fun `static invariants carry the right codes`() {
        ApiError.InvalidResponse.code shouldBe ApiError.LocalCodes.INVALID_RESPONSE
        ApiError.DecodingFailed.code shouldBe ApiError.LocalCodes.DECODING_FAILED
        ApiError.MissingSession.code shouldBe ApiError.LocalCodes.MISSING_SESSION
        ApiError.InvalidPhone.code shouldBe ApiError.LocalCodes.INVALID_PHONE
    }

    // ─── isUnauthorized convenience ───────────────────────────────────

    @Test fun `isUnauthorized is true exactly for status 401`() {
        ApiError(statusCode = 401, message = "x").isUnauthorized shouldBe true
        ApiError(statusCode = 403, message = "x").isUnauthorized shouldBe false
        ApiError(statusCode = null, message = "x").isUnauthorized shouldBe false
    }

    // ─── from() preserves throwable messages ───────────────────────────

    @Test fun `from() preserves the original throwable message`() {
        val original = ConnectException("connection refused on port 443")
        val result = ApiError.from(original)
        result.message shouldContain "connection refused"
    }

    @Test fun `from() falls back to a non-empty message when throwable has no message`() {
        val original = RuntimeException()
        val result = ApiError.from(original)
        // The message slot must always carry SOMETHING the dev fallback layer
        // can show — the UI never resolves to an empty string.
        result.message shouldNotBe ""
    }
}
