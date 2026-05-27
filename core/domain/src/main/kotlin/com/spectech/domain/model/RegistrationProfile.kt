package com.spectech.domain.model

import com.spectech.domain.enums.UserRole

/**
 * Profile fields collected during registration. Sent alongside the OTP
 * verification request. Not @Serializable on its own; flattened into
 * the verify-otp body at the network layer.
 */
data class RegistrationProfile(
    val name: String,
    val email: String?,
    val city: String,
    val role: UserRole,
    val accountType: AccountType,
) {
    enum class AccountType(val wire: String) {
        INDIVIDUAL("individual"),
        LEGAL_ENTITY("legal_entity"),
    }
}
