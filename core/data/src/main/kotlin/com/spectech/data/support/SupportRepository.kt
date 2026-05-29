package com.spectech.data.support

import com.spectech.network.endpoints.SupportApi
import com.spectech.network.endpoints.SupportMessageResponse
import com.spectech.network.http.ApiClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single-shot "Contact support" submission. Mirrors iOS `SupportAPI`.
 * Stateless — the form lives entirely in the view model, with this layer
 * only existing so the wire shape stays out of the feature module.
 */
@Singleton
class SupportRepository @Inject constructor(
    private val api: ApiClient,
) {
    suspend fun sendMessage(
        message: String,
        contactInfo: String?,
        relatedEntityType: String?,
        relatedEntityId: String?,
    ): SupportMessageResponse =
        api.send<SupportMessageResponse>(
            SupportApi.SendMessage(
                message = message,
                contactInfo = contactInfo,
                relatedEntityType = relatedEntityType,
                relatedEntityId = relatedEntityId,
            ),
        ).data
}
