package com.spectech.network.http

import com.spectech.domain.error.ApiError
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class ApiErrorDetail(
    val field: String? = null,
    val message: String? = null,
)

@Serializable
internal data class ApiErrorBody(
    val code: String? = null,
    val message: String? = null,
    val details: List<ApiErrorDetail>? = null,
)

@Serializable
internal data class ApiErrorEnvelope(
    val message: String? = null,
    val error: ApiErrorBody? = null,
)

/**
 * Decodes a non-2xx response body into an [ApiError]. Tolerates both wrapped
 * (`{error: {code, message, details}}`) and top-level (`{message}`) shapes;
 * falls back to [ApiError.fallback] for the status code if neither parses.
 */
fun decodeApiError(statusCode: Int, rawBody: String, json: Json = SpecTechJson): ApiError {
    val parsed = runCatching { json.decodeFromString<ApiErrorEnvelope>(rawBody) }.getOrNull()
    val fallback = ApiError.fallback(statusCode)
    val message = parsed?.error?.message ?: parsed?.message ?: fallback.message
    val details = parsed?.error?.details?.mapNotNull { it.message }?.takeIf { it.isNotEmpty() }
    return ApiError(
        statusCode = statusCode,
        code = parsed?.error?.code,
        message = message,
        details = details,
    )
}
