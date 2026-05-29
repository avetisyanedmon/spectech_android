package com.spectech.network.endpoints

import com.spectech.network.http.ApiTarget
import com.spectech.network.http.HttpMethod
import com.spectech.network.http.SpecTechJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
internal data class UpdateProfileBody(
    val name: String,
    val email: String,
    val city: String,
)

/**
 * `PATCH /users/me/profile` response payload — the canonical user record after
 * the edit. Optional fields tolerate the backend omitting empty strings
 * (matches iOS `UpdateProfileResponse`).
 */
@Serializable
data class UpdateProfileResponse(
    val id: String,
    val phone: String,
    val name: String? = null,
    val email: String? = null,
    val city: String? = null,
    val role: String,
)

sealed interface ProfileApi : ApiTarget {

    data class UpdateProfile(
        val name: String,
        val email: String,
        val city: String,
    ) : ProfileApi {
        override val path: String = "users/me/profile"
        override val method: HttpMethod = HttpMethod.PATCH
        override val requiresAuth: Boolean = true
        override val body: String =
            SpecTechJson.encodeToString(UpdateProfileBody(name, email, city))
    }
}
