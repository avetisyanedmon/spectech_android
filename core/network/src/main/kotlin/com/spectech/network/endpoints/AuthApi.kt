package com.spectech.network.endpoints

import com.spectech.network.http.ApiTarget
import com.spectech.network.http.HttpMethod
import com.spectech.network.http.SpecTechJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

// MARK: - Body / response DTOs ───────────────────────────────────────────────

@Serializable
internal data class SendOtpBody(val phone: String)

/**
 * Verify-OTP body. Optional registration fields are omitted from the wire
 * payload when null (see [SpecTechJson] config: `explicitNulls = false`).
 *
 * iOS does NOT apply snake_case to request keys (its `JSONEncoder` runs with
 * default key strategy), so `account_type` is what the global JsonNamingStrategy
 * produces and what the backend accepts. If a future request endpoint requires
 * camelCase keys explicitly, set `@SerialName` on those fields.
 */
@Serializable
internal data class VerifyOtpBody(
    val phone: String,
    val code: String,
    val name: String? = null,
    val email: String? = null,
    val city: String? = null,
    val role: String? = null,
    val accountType: String? = null,
)

@Serializable
data class SendOtpResponseData(
    val phone: String,
    val expiresIn: Int? = null,
)

@Serializable
data class VerifyOtpUserData(
    val id: String,
    val phone: String,
    val role: String,
    val name: String? = null,
    val email: String? = null,
    val city: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class VerifyOtpResponseData(
    val token: String,
    val user: VerifyOtpUserData,
    val isNewUser: Boolean = false,
)

// MARK: - Endpoint targets ───────────────────────────────────────────────────

sealed interface AuthApi : ApiTarget {
    override val method: HttpMethod get() = HttpMethod.POST
    override val requiresAuth: Boolean get() = false

    data class SendOtp(val phone: String) : AuthApi {
        override val path: String = "auth/send-otp"
        override val body: String = SpecTechJson.encodeToString(SendOtpBody(phone))
    }

    data class VerifyOtp(
        val phone: String,
        val code: String,
        val name: String? = null,
        val email: String? = null,
        val city: String? = null,
        val role: String? = null,
        val accountType: String? = null,
    ) : AuthApi {
        override val path: String = "auth/verify-otp"
        override val body: String = SpecTechJson.encodeToString(
            VerifyOtpBody(
                phone = phone,
                code = code,
                name = name,
                email = email,
                city = city,
                role = role,
                accountType = accountType,
            )
        )
    }
}
