# 01 — API Client

The Android `ApiClient` must produce **byte-identical** HTTP requests (modulo
default headers) compared to iOS so the backend's signature verification and
nonce store both accept them. Read this in conjunction with
[02-hmac-signing.md](02-hmac-signing.md) and
[03-cert-pinning.md](03-cert-pinning.md).

iOS reference: `SpecTechIOS/Networking/API/APIClient.swift`.

## Required behaviors

1. **HMAC-SHA256 signing** of every request — `X-Client-Id`, `X-Timestamp`,
   `X-Nonce`, `X-Signature` headers.
2. **Certificate pinning** on `spectech-backoffice.onrender.com` using two
   intermediate-CA SHA-256 hashes (fail closed).
3. **Bearer token** on authenticated requests (`Authorization: Bearer <jwt>`).
4. **401 → clear session** automatically.
5. **Retries** for 502/503/504 and `URLError.cancelled/timedOut/connectionLost`
   transient failures — up to **2 retries** with **2s × attempt** delay.
6. **Snake-case JSON decoding** (server uses snake_case keys).
7. **ISO-8601 dates** with optional fractional seconds.
8. **APIError envelope decoding**: `{ "error": { "code": "...", "message": "...", "details": [...] } }`
   or `{ "message": "..." }` at top level.
9. **Without-escape-slashes** JSON output (`/` not `\/`) — must match what
   `JSONEncoder.outputFormatting = .withoutEscapingSlashes` produces.

## Ktor-based implementation

```kotlin
@Singleton
class ApiClient @Inject constructor(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend inline fun <reified Resp> send(target: ApiTarget): ApiEnvelope<Resp> =
        client.request(target.toRequest(baseUrl)).body()
    // ApiEnvelope<Resp>: { success: Boolean?, data: Resp }
}
```

```kotlin
@Provides
@Singleton
fun provideHttpClient(
    config: AppConfiguration,
    sessionProvider: SessionProvider,
    json: Json,
): HttpClient = HttpClient(OkHttp) {

    engine {
        config {
            certificatePinner(buildCertificatePinner())   // see cert-pinning.md
            connectTimeout(30, SECONDS)
            readTimeout(120, SECONDS)
        }
    }

    install(ContentNegotiation) { json(json) }

    install(HttpTimeout) {
        requestTimeoutMillis = 120_000
        connectTimeoutMillis = 30_000
    }

    install(HttpRequestRetry) {
        maxRetries = 2
        retryIf { _, response -> response.status.value in setOf(502, 503, 504) }
        retryOnExceptionIf { _, cause ->
            cause is IOException || cause is HttpRequestTimeoutException
        }
        delayMillis(respectRetryAfterHeader = true) { attempt -> 2_000L * attempt }
    }

    install(HttpSendPipelinePhase("AuthAndSignature")) {
        // pseudocode hook ordering
    }

    defaultRequest {
        url(config.apiBaseUrl)
        header(HttpHeaders.Accept, "application/json")
    }

    // 1. Add bearer token (if any) — this MUST run before the signature.
    install("AuthTokenPlugin") {
        onRequest { request, _ ->
            val token = sessionProvider.authToken()
            if (token != null) request.header(HttpHeaders.Authorization, "Bearer $token")
            else if (request.attributes.contains(REQUIRES_AUTH_KEY)
                  && request.attributes[REQUIRES_AUTH_KEY]) {
                throw ApiError(statusCode = 401, message = "Authentication required.")
            }
        }
    }

    // 2. Sign request — last hook before the engine sends. See hmac-signing.md
    install(HmacSigningPlugin(config.clientId, config.clientSecret))

    // 3. 401 handler — clear session
    expectSuccess = false
    HttpResponseValidator {
        handleResponseExceptionWithRequest { cause, _ -> /* leave alone */ }
        validateResponse { response ->
            if (response.status.value == 401) sessionProvider.clearSession()
            if (response.status.value !in 200..299) {
                throw decodeApiError(response, json)
            }
        }
    }
}
```

### kotlinx.serialization config

```kotlin
@Provides @Singleton
fun provideJson(): Json = Json {
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy.SnakeCase
    coerceInputValues = true
    encodeDefaults = false
    explicitNulls = false
    serializersModule = SerializersModule {
        contextual(Uuid::class, UuidSerializer)
        contextual(Instant::class, InstantIso8601Serializer)
    }
}
```

`JsonNamingStrategy.SnakeCase` is the analog of iOS's
`JSONDecoder.keyDecodingStrategy = .convertFromSnakeCase`.

### InstantIso8601Serializer

Default kotlinx-datetime parses ISO-8601 with or without fractional seconds —
no extra work needed.

### ApiEnvelope

```kotlin
@Serializable
data class ApiEnvelope<T>(val success: Boolean? = null, val data: T)
```

Every endpoint returns the envelope. Repositories unwrap `.data` before
returning to view models.

## ApiTarget

Single seal for every endpoint. Direct port of iOS `APITarget` protocol.

```kotlin
sealed interface ApiTarget {
    val path: String
    val method: HttpMethod
    val queryItems: List<Pair<String, String>> get() = emptyList()
    val body: ByteArray? get() = null
    val requiresAuth: Boolean get() = true
    val contentType: ContentType? get() = null

    fun toRequest(baseUrl: String): HttpRequestBuilder = HttpRequestBuilder().apply {
        url(baseUrl.trimEnd('/') + "/" + path.trimStart('/'))
        method = this@ApiTarget.method
        queryItems.forEach { (k, v) -> parameter(k, v) }
        body?.let {
            setBody(it)
            header(HttpHeaders.ContentType, (contentType ?: ContentType.Application.Json).toString())
        }
        attributes.put(REQUIRES_AUTH_KEY, requiresAuth)
    }
}

val REQUIRES_AUTH_KEY = AttributeKey<Boolean>("REQUIRES_AUTH")
```

## ApiError

Sealed-ish data class (a simple data class is fine — iOS uses one too).

```kotlin
data class ApiError(
    val statusCode: Int? = null,
    val code: String? = null,
    override val message: String,
    val details: List<String>? = null,
) : RuntimeException(message) {
    val isUnauthorized: Boolean get() = statusCode == 401

    companion object {
        val InvalidResponse = ApiError(message = "The server returned an invalid response.")
        val DecodingFailed = ApiError(message = "The app could not read the server response.")
        val MissingSession = ApiError(message = "You need to sign in to continue.")
        val InvalidPhone = ApiError(message = "Enter a valid Russian phone number.")

        fun fallback(statusCode: Int): ApiError = when (statusCode) {
            400 -> ApiError(400, message = "Some fields are invalid. Please review the form and try again.")
            401 -> ApiError(401, message = "Your session expired. Sign in again.")
            403 -> ApiError(403, message = "You do not have permission to perform this action.")
            404 -> ApiError(404, message = "The requested resource was not found.")
            429 -> ApiError(429, message = "Too many attempts. Please wait before trying again.")
            503 -> ApiError(503, message = "The SMS provider is temporarily unavailable. Try again later.")
            else -> ApiError(statusCode, message = "The server returned an unexpected error.")
        }

        fun from(throwable: Throwable): ApiError = (throwable as? ApiError)
            ?: ApiError(message = throwable.localizedMessage ?: "Unknown error")
    }
}

@Serializable
private data class ApiErrorBody(val code: String? = null, val message: String? = null, val details: List<ApiErrorDetail>? = null)
@Serializable
private data class ApiErrorDetail(val field: String? = null, val message: String? = null)
@Serializable
private data class ApiErrorEnvelope(val message: String? = null, val error: ApiErrorBody? = null)

internal suspend fun decodeApiError(response: HttpResponse, json: Json): ApiError {
    val raw = response.bodyAsText()
    val parsed = runCatching { json.decodeFromString<ApiErrorEnvelope>(raw) }.getOrNull()
    val fallback = ApiError.fallback(response.status.value)
    val message = parsed?.error?.message ?: parsed?.message ?: fallback.message
    val details = parsed?.error?.details?.mapNotNull { it.message }
    return ApiError(response.status.value, parsed?.error?.code, message, details)
}
```

## Retry rules

Match iOS exactly:

| Case | Action |
|---|---|
| Status 502, 503, 504 | retry up to 2 times, delay = `2_000 ms × attempt` |
| Network timeout | retry |
| Connection lost | retry |
| Cannot connect to host | retry |
| Other 4xx / 5xx | throw immediately |

iOS implements this in a manual `for attempt in 0...maxRetries` loop. Ktor
already has `install(HttpRequestRetry) { … }` — use it.

## Logging

Mirror `APILogger` (DEBUG-only, redact sensitive headers).

```kotlin
install(Logging) {
    if (BuildConfig.DEBUG) {
        level = LogLevel.ALL
        logger = TimberLogger(redactedHeaders = setOf("Authorization", "X-Signature"))
    } else level = LogLevel.NONE
}
```
