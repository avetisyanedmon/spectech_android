package com.spectech.uikit.strings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.spectech.domain.error.ApiError
import com.spectech.uikit.R

/**
 * Maps an [ApiError]'s stable [ApiError.code] to a localized string. Backend
 * errors (whose `code` doesn't match any [ApiError.LocalCodes] value, or whose
 * code is null) fall back to the server-supplied [ApiError.message] which is
 * already localized via the `Accept-Language` header.
 *
 * Resolves the EN/RU resource at call time so the user's current locale is
 * honoured — switching language in [`features/profile/.../LanguagePickerSheet.kt`]
 * applies immediately on the next recomposition.
 */
@Composable
fun ApiError.localizedMessage(): String {
    val resId = resourceForCode(code)
    return if (resId != null) stringResource(resId) else message
}

/**
 * Context-bound overload for non-Composable call sites (e.g. one-shot
 * Toast/Snackbar emissions). Prefer the Composable version inside UI trees.
 */
fun ApiError.localizedMessage(context: Context): String {
    val resId = resourceForCode(code)
    return if (resId != null) context.getString(resId) else message
}

private fun resourceForCode(code: String?): Int? = when (code) {
    ApiError.LocalCodes.INVALID_RESPONSE -> R.string.error_invalid_response
    ApiError.LocalCodes.DECODING_FAILED -> R.string.error_decoding_failed
    ApiError.LocalCodes.MISSING_SESSION -> R.string.error_missing_session
    ApiError.LocalCodes.INVALID_PHONE -> R.string.error_invalid_phone
    ApiError.LocalCodes.AUTH_REQUIRED -> R.string.error_auth_required
    ApiError.LocalCodes.URL_BUILD_FAILED -> R.string.error_url_build_failed
    ApiError.LocalCodes.GENERIC_UNKNOWN -> R.string.error_generic_unknown
    ApiError.LocalCodes.OTP_INVALID_OR_EXPIRED -> R.string.error_otp_invalid_or_expired
    ApiError.LocalCodes.OTP_RATE_LIMITED -> R.string.error_otp_rate_limited
    ApiError.LocalCodes.FALLBACK_400 -> R.string.error_fallback_400
    ApiError.LocalCodes.FALLBACK_401 -> R.string.error_fallback_401
    ApiError.LocalCodes.FALLBACK_403 -> R.string.error_fallback_403
    ApiError.LocalCodes.FALLBACK_404 -> R.string.error_fallback_404
    ApiError.LocalCodes.FALLBACK_429 -> R.string.error_fallback_429
    ApiError.LocalCodes.FALLBACK_503 -> R.string.error_fallback_503
    ApiError.LocalCodes.FALLBACK_OTHER -> R.string.error_fallback_other
    ApiError.LocalCodes.GARAGE_CATEGORY_REQUIRED -> R.string.error_garage_category_required
    ApiError.LocalCodes.GARAGE_PHOTO_READ_FAILED -> R.string.error_garage_photo_read_failed
    ApiError.LocalCodes.NETWORK_OFFLINE -> R.string.error_network_offline
    ApiError.LocalCodes.SSL_FAILURE -> R.string.error_ssl_failure
    ApiError.LocalCodes.SERVER_TIMEOUT -> R.string.error_server_timeout
    else -> null
}

/**
 * Convenience for screens that just need a `@Composable` source of text and
 * already have the [ApiError] in hand. Equivalent to calling
 * [localizedMessage] directly.
 */
@Composable
fun localizedApiErrorMessage(error: ApiError): String = error.localizedMessage()

/**
 * Resolves the message for any non-Composable site that has a
 * [LocalContext] reference but wants a uniform call site.
 */
fun localizedApiErrorMessage(context: Context, error: ApiError): String =
    error.localizedMessage(context)
