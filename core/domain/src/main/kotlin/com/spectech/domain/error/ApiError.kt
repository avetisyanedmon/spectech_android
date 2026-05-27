package com.spectech.domain.error

/**
 * Public error type for everything that goes wrong talking to the SpecTech
 * backend. Mirrors iOS `APIError` (SpecTechIOS/Networking/API/APIError.swift)
 * including its static fallback table.
 *
 * NOTE: not @Serializable; this is a thrown exception, decoded from the wire
 * by `ApiErrorEnvelope` in core/network.
 */
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
            ?: ApiError(message = throwable.localizedMessage ?: throwable.message ?: "Unknown error")
    }
}
