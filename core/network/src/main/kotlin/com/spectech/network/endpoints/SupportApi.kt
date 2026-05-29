package com.spectech.network.endpoints

import com.spectech.network.http.ApiTarget
import com.spectech.network.http.HttpMethod
import com.spectech.network.http.SpecTechJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
internal data class SupportMessageBody(
    val message: String,
    val contactInfo: String? = null,
    val relatedEntityType: String? = null,
    val relatedEntityId: String? = null,
)

@Serializable
data class SupportMessageResponse(
    val id: String? = null,
    val status: String? = null,
)

/**
 * "Contact support" endpoint. Public (no auth) so signed-out users can still
 * reach out. When called by an authenticated user the caller layers
 * `contactInfo = currentUser.phone` so support knows who to reply to.
 */
sealed interface SupportApi : ApiTarget {

    data class SendMessage(
        val message: String,
        val contactInfo: String? = null,
        val relatedEntityType: String? = null,
        val relatedEntityId: String? = null,
    ) : SupportApi {
        override val path: String = "support/messages"
        override val method: HttpMethod = HttpMethod.POST
        override val requiresAuth: Boolean = false
        override val body: String = SpecTechJson.encodeToString(
            SupportMessageBody(
                message = message,
                contactInfo = contactInfo,
                relatedEntityType = relatedEntityType,
                relatedEntityId = relatedEntityId,
            ),
        )
    }
}
