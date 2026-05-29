package com.spectech.network.endpoints

import com.spectech.network.http.ApiTarget
import com.spectech.network.http.HttpMethod
import com.spectech.network.http.SpecTechJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * Wire-shape for the user's saved marketplace filter. The backend sanitizes
 * each array against its canonical maps (`EQUIPMENT_CATEGORY_MAP`,
 * `PRICING_UNIT_MAP`, `PAYMENT_TYPE_MAP`) so we send the same snake_case
 * codes the marketplace list query already accepts. Mirrors iOS
 * `SavedFilterPayload`.
 */
@Serializable
data class SavedFilterPayload(
    val categories: List<String>,
    val regions: List<String>,
    val cities: List<String>,
    val pricingUnits: List<String>,
    val paymentTypes: List<String>,
)

@Serializable
internal data class UpsertSavedFilterBody(
    val filters: SavedFilterPayload,
    val enabled: Boolean,
)

@Serializable
internal data class SetSavedFilterEnabledBody(val enabled: Boolean)

@Serializable
data class SavedFilterResponse(
    val filters: SavedFilterPayload? = null,
    val enabled: Boolean? = null,
    val updatedAt: String? = null,
)

@Serializable
data class SavedFilterDeletedResponse(val deleted: Boolean? = null)

/**
 * Saved-filter endpoints — the backend matcher fans out a push to every
 * enabled subscription when a new order matches the saved filter.
 *
 *   GET    /notifications/saved-filter           — fetch (404 if none)
 *   PUT    /notifications/saved-filter           — upsert filter + enabled
 *   POST   /notifications/saved-filter/enabled   — toggle enabled flag
 *   DELETE /notifications/saved-filter           — clear
 */
sealed interface SavedFilterApi : ApiTarget {

    data object Fetch : SavedFilterApi {
        override val path: String = "notifications/saved-filter"
        override val method: HttpMethod = HttpMethod.GET
        override val requiresAuth: Boolean = true
    }

    data class Upsert(val payload: SavedFilterPayload, val enabled: Boolean) : SavedFilterApi {
        override val path: String = "notifications/saved-filter"
        override val method: HttpMethod = HttpMethod.PUT
        override val requiresAuth: Boolean = true
        override val body: String =
            SpecTechJson.encodeToString(UpsertSavedFilterBody(payload, enabled))
    }

    data class SetEnabled(val enabled: Boolean) : SavedFilterApi {
        override val path: String = "notifications/saved-filter/enabled"
        override val method: HttpMethod = HttpMethod.POST
        override val requiresAuth: Boolean = true
        override val body: String =
            SpecTechJson.encodeToString(SetSavedFilterEnabledBody(enabled))
    }

    data object Delete : SavedFilterApi {
        override val path: String = "notifications/saved-filter"
        override val method: HttpMethod = HttpMethod.DELETE
        override val requiresAuth: Boolean = true
    }
}
