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
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import io.ktor.util.appendIfNameAbsent
import java.io.IOException

private val REQUIRES_AUTH = AttributeKey<Boolean>("RequiresAuth")

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
 * attempt linear backoff (matches iOS APIClient retry policy).
 */
fun buildHttpClient(
    clientId: String,
    clientSecret: String,
    sessionProvider: SessionProvider,
    enableLogging: Boolean = false,
): HttpClient = HttpClient(OkHttp) {
    expectSuccess = false

    engine {
        config {
            connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            addInterceptor(HmacInterceptor(clientId, clientSecret))
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
        retryIf { _, response -> response.status.value in setOf(502, 503, 504) }
        retryOnExceptionIf { _, cause -> cause is IOException }
        delayMillis(respectRetryAfterHeader = true) { attempt -> 2_000L * attempt }
    }

    install(createClientPlugin("BearerAuthPlugin") {
        onRequest { request, _ ->
            val token = sessionProvider.authToken()
            if (!token.isNullOrEmpty()) {
                request.headers.appendIfNameAbsent(HttpHeaders.Authorization, "Bearer $token")
            } else if (request.attributes.getOrNull(REQUIRES_AUTH) == true) {
                throw ApiError(statusCode = 401, message = "Authentication required.")
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
