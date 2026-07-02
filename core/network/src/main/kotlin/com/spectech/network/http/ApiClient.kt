package com.spectech.network.http

import com.spectech.domain.error.ApiError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import io.ktor.util.appendIfNameAbsent
import java.io.IOException
import okhttp3.CertificatePinner

private val REQUIRES_AUTH = AttributeKey<Boolean>("RequiresAuth")

/**
 * Production host whose TLS chain we pin against MITM. Matches iOS
 * `APIClient.pinnedHost`. Pinning is gated by the [AppConfiguration.pinCertificates]
 * flag so debug builds running through Charles/mitmproxy can opt out.
 */
private const val PINNED_HOST = "spectech-backoffice.onrender.com"

/**
 * SHA-256 of the DER-encoded intermediate CA certificates in Render's chain.
 * Mirrors `APIClient.pinnedCertHashes` in
 * SpecTechIOS/Networking/API/APIClient.swift exactly so both platforms accept
 * (and reject) the same set of server certificates.
 *
 * Intermediates are pinned (not the leaf) so pins survive leaf certificate
 * rotation. Refresh both lists in lockstep when GTS rotates intermediates.
 */
private val PINNED_SHA256_BASE64 = listOf(
    "HfwWBfutNY2LyET3bRUgP6ycpcGnn9SFf/ryhk++v5Y=", // Google Trust Services WE1
    "drJ7gKWAJ9w88dpo2sFwEO2TmX0LYD4vrb6FASSTtac=", // GTS Root R4
)

/**
 * Public HTTP entry point. Constructed once by Hilt (see core/data NetworkModule).
 *
 * Each call:
 *   1. URL = baseUrl + target.path
 *   2. Method, query params, body (pre-serialized JSON) from the target
 *   3. Bearer token attached automatically when [SessionProvider.authToken] is non-null
 *   4. HMAC headers attached by the OkHttp interceptor underneath
 *   5. Non-2xx → typed [ApiError]
 *   6. 401 → clears the session via the same provider
 *
 * Mirrors `SpecTechIOS/Networking/API/APIClient.swift` behaviour.
 */
class ApiClient(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend inline fun <reified R> send(target: ApiTarget): ApiEnvelope<R> =
        sendInternal(target).body()

    @PublishedApi
    internal suspend fun sendInternal(target: ApiTarget): HttpResponse =
        client.request {
            url(buildUrl(baseUrl, target.path))
            method = target.method.toKtor()
            target.queryItems.forEach { (k, v) -> parameter(k, v) }
            target.body?.let { jsonString ->
                val ct = ContentType.parse(target.contentType ?: ContentType.Application.Json.toString())
                setBody(TextContent(jsonString, ct))
            }
            attributes.put(REQUIRES_AUTH, target.requiresAuth)
        }

    companion object {
        @PublishedApi internal fun buildUrl(base: String, path: String): String {
            val cleanBase = base.trimEnd('/')
            val cleanPath = path.trimStart('/')
            return "$cleanBase/$cleanPath"
        }
    }
}

/**
 * Builds the Ktor HttpClient with the full Phase 1 pipeline. Pure-JVM factory
 * so the network module stays free of Android dependencies; the Hilt module in
 * core/data calls this with the right DI inputs.
 *
 * Retries 502/503/504 and transient IO errors up to two times with a 2s ×
 * attempt linear backoff — GET requests only, so a lost response can never
 * duplicate a non-idempotent write (order/bid creation).
 */
fun buildHttpClient(
    clientId: String,
    clientSecret: String,
    sessionProvider: SessionProvider,
    enableLogging: Boolean = false,
    pinCertificates: Boolean = false,
): HttpClient = HttpClient(OkHttp) {
    expectSuccess = false

    engine {
        config {
            connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            addInterceptor(HmacInterceptor(clientId, clientSecret))

            // Match iOS PinningDelegate: validate the server cert chain against
            // the GTS intermediates whose SHA-256 fingerprints we ship in-app.
            // OkHttp's pinner fails the TLS handshake on mismatch, mirroring
            // iOS' `cancelAuthenticationChallenge` behaviour.
            if (pinCertificates) {
                val pinner = CertificatePinner.Builder().apply {
                    PINNED_SHA256_BASE64.forEach { pin ->
                        add(PINNED_HOST, "sha256/$pin")
                    }
                }.build()
                certificatePinner(pinner)
            }
        }
    }

    install(ContentNegotiation) {
        json(SpecTechJson)
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 120_000
        connectTimeoutMillis = 30_000
    }

    install(HttpRequestRetry) {
        maxRetries = 2
        // Only GET is safe to replay. A POST that dies with a timeout or a
        // gateway 502/503/504 may have already been processed by the app
        // server — re-sending it would duplicate an order or bid.
        retryIf { request, response ->
            request.method == HttpMethod.Get && response.status.value in setOf(502, 503, 504)
        }
        retryOnExceptionIf { request, cause ->
            request.method == HttpMethod.Get && cause is IOException
        }
        delayMillis(respectRetryAfterHeader = true) { attempt -> 2_000L * attempt }
    }

    install(createClientPlugin("BearerAuthPlugin") {
        onRequest { request, _ ->
            val token = sessionProvider.authToken()
            if (!token.isNullOrEmpty()) {
                request.headers.appendIfNameAbsent(HttpHeaders.Authorization, "Bearer $token")
            } else if (request.attributes.getOrNull(REQUIRES_AUTH) == true) {
                throw ApiError(
                    statusCode = 401,
                    code = ApiError.LocalCodes.AUTH_REQUIRED,
                    message = "Authentication required.",
                )
            }
        }
    })

    defaultRequest {
        headers.appendIfNameAbsent(HttpHeaders.Accept, "application/json")
    }

    HttpResponseValidator {
        validateResponse { response ->
            val status = response.status.value
            if (status !in 200..299) {
                val body = response.bodyAsText()
                val err = decodeApiError(status, body)
                if (err.isUnauthorized) sessionProvider.clearSession()
                throw err
            }
        }
        handleResponseExceptionWithRequest { cause, _ -> throw cause }
    }
}
