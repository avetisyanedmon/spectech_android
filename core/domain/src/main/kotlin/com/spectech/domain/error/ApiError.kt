package com.spectech.domain.error

/**
 * Public error type for everything that goes wrong talking to the SpecTech
 * backend. Mirrors iOS `APIError` (SpecTechIOS/Networking/API/APIError.swift)
 * including its static fallback table.
 *
 * NOTE: not @Serializable; this is a thrown exception, decoded from the wire
 * by `ApiErrorEnvelope` in core/network.
 *
 * **Localization** (Section 2): client-side error messages carry a stable
 * [code] from [LocalCodes] so the UI layer can resolve a `stringResource`
 * without `Context` leaking into the domain. UI consumers call the
 * `core/ui-kit` extension `ApiError.localizedMessage(context)` which maps the
 * code to a localized resource (RU + EN) and falls back to [message] when no
 * mapping exists. Backend-returned errors keep their server-supplied,
 * already-localized text via `Accept-Language`.
 */
data class ApiError(
    val statusCode: Int? = null,
    val code: String? = null,
    override val message: String,
    val details: List<String>? = null,
) : RuntimeException(message) {

    val isUnauthorized: Boolean get() = statusCode == 401

    /**
     * Stable code namespace for client-side ApiError instances. UI maps each
     * code to a `R.string.error_*` resource. New codes added here must get a
     * matching resource + RU translation in `core/ui-kit/values/strings.xml`.
     *
     * Wire codes from the backend (lowerCamelCase) are passed through verbatim
     * in [code]; this namespace uses `LOCAL_` prefix to avoid collisions.
     */
    object LocalCodes {
        const val INVALID_RESPONSE = "LOCAL_INVALID_RESPONSE"
        const val DECODING_FAILED = "LOCAL_DECODING_FAILED"
        const val MISSING_SESSION = "LOCAL_MISSING_SESSION"
        const val INVALID_PHONE = "LOCAL_INVALID_PHONE"
        const val AUTH_REQUIRED = "LOCAL_AUTH_REQUIRED"
        const val URL_BUILD_FAILED = "LOCAL_URL_BUILD_FAILED"
        const val GENERIC_UNKNOWN = "LOCAL_GENERIC_UNKNOWN"

        // OTP/auth heuristic errors emitted by AuthRepository.toAuthError.
        const val OTP_INVALID_OR_EXPIRED = "LOCAL_OTP_INVALID_OR_EXPIRED"
        const val OTP_RATE_LIMITED = "LOCAL_OTP_RATE_LIMITED"

        // HTTP status-code fallback codes.
        const val FALLBACK_400 = "LOCAL_FALLBACK_400"
        const val FALLBACK_401 = "LOCAL_FALLBACK_401"
        const val FALLBACK_403 = "LOCAL_FALLBACK_403"
        const val FALLBACK_404 = "LOCAL_FALLBACK_404"
        const val FALLBACK_429 = "LOCAL_FALLBACK_429"
        const val FALLBACK_503 = "LOCAL_FALLBACK_503"
        const val FALLBACK_OTHER = "LOCAL_FALLBACK_OTHER"

        // Garage validation failures emitted by Add/Edit equipment VMs.
        const val GARAGE_CATEGORY_REQUIRED = "LOCAL_GARAGE_CATEGORY_REQUIRED"
        const val GARAGE_PHOTO_READ_FAILED = "LOCAL_GARAGE_PHOTO_READ_FAILED"

        // Classified by [ApiError.from] from typed JDK / kotlinx exceptions.
        const val NETWORK_OFFLINE = "LOCAL_NETWORK_OFFLINE"
        const val SSL_FAILURE = "LOCAL_SSL_FAILURE"
        const val SERVER_TIMEOUT = "LOCAL_SERVER_TIMEOUT"
    }

    companion object {
        val InvalidResponse = ApiError(
            code = LocalCodes.INVALID_RESPONSE,
            message = "The server returned an invalid response.",
        )
        val DecodingFailed = ApiError(
            code = LocalCodes.DECODING_FAILED,
            message = "The app could not read the server response.",
        )
        val MissingSession = ApiError(
            code = LocalCodes.MISSING_SESSION,
            message = "You need to sign in to continue.",
        )
        val InvalidPhone = ApiError(
            code = LocalCodes.INVALID_PHONE,
            message = "Enter a valid Russian phone number.",
        )

        fun fallback(statusCode: Int): ApiError = when (statusCode) {
            400 -> ApiError(400, code = LocalCodes.FALLBACK_400, message = "Some fields are invalid. Please review the form and try again.")
            401 -> ApiError(401, code = LocalCodes.FALLBACK_401, message = "Your session expired. Sign in again.")
            403 -> ApiError(403, code = LocalCodes.FALLBACK_403, message = "You do not have permission to perform this action.")
            404 -> ApiError(404, code = LocalCodes.FALLBACK_404, message = "The requested resource was not found.")
            429 -> ApiError(429, code = LocalCodes.FALLBACK_429, message = "Too many attempts. Please wait before trying again.")
            503 -> ApiError(503, code = LocalCodes.FALLBACK_503, message = "The SMS provider is temporarily unavailable. Try again later.")
            else -> ApiError(statusCode, code = LocalCodes.FALLBACK_OTHER, message = "The server returned an unexpected error.")
        }

        /**
         * Classifies arbitrary throwables into typed [ApiError]s so the UI can
         * resolve a specific localized string instead of always falling back
         * to a generic "something went wrong" banner.
         *
         * Walks the cause chain because Ktor and OkHttp wrap the underlying
         * JDK exception (e.g. `IOException("javax.net.ssl.SSLHandshakeException: ...")`).
         */
        fun from(throwable: Throwable): ApiError {
            (throwable as? ApiError)?.let { return it }
            // Look for the most specific cause we can classify.
            var cause: Throwable? = throwable
            while (cause != null) {
                val klass = cause.javaClass.name
                when {
                    klass.contains("SSL", ignoreCase = false) ||
                        klass.contains("CertPath", ignoreCase = false) ||
                        klass.contains("CertificatePinner", ignoreCase = false) ->
                        return ApiError(
                            code = LocalCodes.SSL_FAILURE,
                            message = cause.message ?: "TLS handshake failed.",
                        )
                    klass.contains("Timeout", ignoreCase = false) ||
                        klass == "java.net.SocketTimeoutException" ->
                        return ApiError(
                            code = LocalCodes.SERVER_TIMEOUT,
                            message = cause.message ?: "The server took too long to respond.",
                        )
                    klass == "java.net.UnknownHostException" ||
                        klass == "java.net.ConnectException" ||
                        klass == "java.net.NoRouteToHostException" ->
                        return ApiError(
                            code = LocalCodes.NETWORK_OFFLINE,
                            message = cause.message ?: "Network unavailable.",
                        )
                    klass.startsWith("kotlinx.serialization.") ->
                        return ApiError(
                            code = LocalCodes.DECODING_FAILED,
                            message = cause.message ?: "Could not decode server response.",
                        )
                }
                cause = cause.cause
            }
            // Plain IOException without one of the known subclasses → treat as offline-ish.
            if (throwable is java.io.IOException) {
                return ApiError(
                    code = LocalCodes.NETWORK_OFFLINE,
                    message = throwable.message ?: "Network unavailable.",
                )
            }
            return ApiError(
                code = LocalCodes.GENERIC_UNKNOWN,
                message = throwable.localizedMessage ?: throwable.message ?: "Unknown error",
            )
        }
    }
}
