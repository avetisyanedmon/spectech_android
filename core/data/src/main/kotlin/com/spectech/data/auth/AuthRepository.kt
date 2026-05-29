package com.spectech.data.auth

import com.spectech.domain.enums.UserRole
import com.spectech.domain.error.ApiError
import com.spectech.domain.model.AuthSession
import com.spectech.domain.model.RegistrationProfile
import com.spectech.domain.model.User
import com.spectech.domain.util.PhoneNormalizer
import com.spectech.network.endpoints.AuthApi
import com.spectech.network.endpoints.SendOtpResponseData
import com.spectech.network.endpoints.VerifyOtpResponseData
import com.spectech.network.http.ApiClient
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps the OTP send/verify endpoints. Maps wire DTOs onto the domain
 * `AuthSession` and hands the result to [SessionStore]. Direct port of iOS
 * `AuthService` (SpecTechIOS/Features/Auth/AuthService.swift).
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiClient,
    private val sessionStore: SessionStore,
) {
    /** Sends an OTP; returns the normalized E.164 phone the user must verify. */
    suspend fun startAuth(rawPhone: String): String {
        val phone = PhoneNormalizer.normalizeRussian(rawPhone)
        val envelope = api.send<SendOtpResponseData>(AuthApi.SendOtp(phone))
        return envelope.data.phone
    }

    /**
     * Verifies the OTP and persists the resulting [AuthSession]. When called
     * from a sign-in flow [profile] is null; from registration it carries the
     * extra fields the backend requires to create the account.
     */
    suspend fun verifyAuth(
        phone: String,
        code: String,
        profile: RegistrationProfile? = null,
    ): AuthSession {
        val envelope = api.send<VerifyOtpResponseData>(
            AuthApi.VerifyOtp(
                phone = phone,
                code = code,
                name = profile?.name,
                email = profile?.email?.takeIf { it.isNotBlank() },
                city = profile?.city,
                role = profile?.role?.wire,
                accountType = profile?.accountType?.wire,
            )
        )
        val data = envelope.data
        val session = AuthSession(
            token = data.token,
            user = User(
                id = data.user.id,
                phone = data.user.phone,
                role = UserRole.fromWire(data.user.role),
                name = data.user.name,
                email = data.user.email,
                city = data.user.city,
                createdAt = data.user.createdAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
            ),
            isNewUser = data.isNewUser,
        )
        sessionStore.save(session)
        return session
    }

    suspend fun logout() = sessionStore.clearSession()
}

/**
 * Translates a low-level exception to an [ApiError] with a friendly message
 * and a stable [ApiError.LocalCodes] code so the UI layer can resolve the
 * localized string. English values are developer fallbacks for the rare cases
 * where the UI has no code mapping.
 */
internal fun Throwable.toAuthError(): ApiError {
    if (this is ApiError) return this
    val msg = (localizedMessage ?: message ?: "").lowercase()
    return when {
        "rate" in msg || "429" in msg || "too many" in msg ->
            ApiError(
                statusCode = 429,
                code = ApiError.LocalCodes.OTP_RATE_LIMITED,
                message = "Too many attempts. Please wait before trying again.",
            )
        "invalid" in msg || "expired" in msg || "token" in msg ->
            ApiError(
                statusCode = 401,
                code = ApiError.LocalCodes.OTP_INVALID_OR_EXPIRED,
                message = "The code is invalid or has expired. Please try again.",
            )
        "phone" in msg || "number" in msg ->
            ApiError(
                code = ApiError.LocalCodes.INVALID_PHONE,
                message = "Enter a valid Russian phone number.",
            )
        else -> ApiError(
            code = ApiError.LocalCodes.GENERIC_UNKNOWN,
            message = localizedMessage ?: message ?: "Unknown error",
        )
    }
}
