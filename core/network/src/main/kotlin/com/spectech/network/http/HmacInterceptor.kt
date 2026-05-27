package com.spectech.network.http

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Signs every outgoing request with the same HMAC-SHA256 scheme iOS uses (see
 * SpecTechIOS/Networking/API/APIClient.swift `signRequest(_:url:)`):
 *
 *     payload = METHOD\nPATH_AND_QUERY\nTIMESTAMP\nNONCE\nBODY_HASH
 *     X-Client-Id, X-Timestamp, X-Nonce, X-Signature headers are added.
 *
 * Notes that must stay aligned with iOS:
 *   - Empty / non-JSON bodies hash the empty string (the server only captures
 *     `req.rawBody` for application/json via express.json's verify hook).
 *   - Path includes the percent-encoded path AND the percent-encoded query
 *     string (?-prefixed), but never the scheme/host.
 *   - Nonce is 16 random bytes hex-encoded (lowercase, 32 chars).
 *   - Timestamp is Unix milliseconds, base-10, as a string.
 *
 * Installed as an OkHttp interceptor so it sees the final serialized body
 * after Ktor has done its content-negotiation work.
 */
class HmacInterceptor(
    private val clientId: String,
    clientSecret: String,
) : Interceptor {

    private val secretKey: SecretKeySpec = SecretKeySpec(
        clientSecret.toByteArray(Charsets.UTF_8),
        HMAC_ALGORITHM,
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val (request, bodyBytes) = chain.request().bufferJsonBody()
        val signed = signRequest(request, bodyBytes)
        return chain.proceed(signed)
    }

    private fun signRequest(request: Request, signableBody: ByteArray): Request {
        val timestamp = System.currentTimeMillis().toString()
        val nonce = randomHex(NONCE_BYTES)

        val method = request.method.uppercase()
        val pathAndQuery = buildPathAndQuery(request)
        val bodyHash = sha256Hex(signableBody)
        val payload = "$method\n$pathAndQuery\n$timestamp\n$nonce\n$bodyHash"

        val signature = hmacSha256Hex(payload)

        return request.newBuilder()
            .header(HEADER_CLIENT_ID, clientId)
            .header(HEADER_TIMESTAMP, timestamp)
            .header(HEADER_NONCE, nonce)
            .header(HEADER_SIGNATURE, signature)
            .build()
    }

    // ─── helpers ───────────────────────────────────────────────────────────

    private fun buildPathAndQuery(request: Request): String {
        val url = request.url
        val path = url.encodedPath
        val query = url.encodedQuery
        return if (query.isNullOrEmpty()) path else "$path?$query"
    }

    private fun hmacSha256Hex(payload: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM).apply { init(secretKey) }
        return mac.doFinal(payload.toByteArray(Charsets.UTF_8)).toHex()
    }

    companion object {
        const val HEADER_CLIENT_ID = "X-Client-Id"
        const val HEADER_TIMESTAMP = "X-Timestamp"
        const val HEADER_NONCE = "X-Nonce"
        const val HEADER_SIGNATURE = "X-Signature"
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val NONCE_BYTES = 16

        internal fun sha256Hex(data: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(data).toHex()

        internal fun ByteArray.toHex(): String =
            joinToString("") { "%02x".format(it) }

        internal fun randomHex(byteCount: Int): String {
            val bytes = ByteArray(byteCount)
            SecureRandom().nextBytes(bytes)
            return bytes.toHex()
        }

        private val EMPTY = ByteArray(0)

        /**
         * Reads the request body into memory (if it's `application/json` and
         * non-empty) and rebuilds the request with an equivalent fresh
         * RequestBody so OkHttp can still write it to the wire. Returns the
         * bytes used for signature hashing — empty for non-JSON or absent
         * bodies, matching the iOS condition.
         */
        internal fun Request.bufferJsonBody(): Pair<Request, ByteArray> {
            val body = this.body ?: return this to EMPTY
            val contentType = body.contentType()
            val ctLower = contentType?.toString()?.lowercase().orEmpty()
            if (!ctLower.startsWith("application/json")) return this to EMPTY

            val sink = Buffer()
            body.writeTo(sink)
            val bytes = sink.readByteArray()
            val rebuiltBody = bytes.toRequestBody(contentType)
            val rebuiltRequest = newBuilder()
                .method(method, rebuiltBody)
                .build()
            return rebuiltRequest to bytes
        }
    }
}
