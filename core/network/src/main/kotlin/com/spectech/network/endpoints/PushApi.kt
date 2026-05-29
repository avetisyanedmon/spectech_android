package com.spectech.network.endpoints

import com.spectech.network.http.ApiTarget
import com.spectech.network.http.HttpMethod
import com.spectech.network.http.SpecTechJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
internal data class RegisterTokenBody(val token: String, val platform: String)

@Serializable
internal data class UnregisterTokenBody(val token: String)

@Serializable
data class RegisterTokenResponse(val registered: Boolean? = null)

@Serializable
data class UnregisterTokenResponse(val unregistered: Boolean? = null)

/**
 * Push notification endpoints. The backend binds the device token to the
 * current user; calls are auth-required so the server knows who to address
 * when a push fires. iOS sends `platform = "ios"`; Android sends `"android"`.
 */
sealed interface PushApi : ApiTarget {

    data class RegisterToken(val token: String, val platform: String = "android") : PushApi {
        override val path: String = "notifications/register"
        override val method: HttpMethod = HttpMethod.POST
        override val requiresAuth: Boolean = true
        override val body: String =
            SpecTechJson.encodeToString(RegisterTokenBody(token, platform))
    }

    data class UnregisterToken(val token: String) : PushApi {
        override val path: String = "notifications/unregister"
        override val method: HttpMethod = HttpMethod.POST
        override val requiresAuth: Boolean = true
        override val body: String =
            SpecTechJson.encodeToString(UnregisterTokenBody(token))
    }
}
