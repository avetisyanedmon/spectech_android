package com.spectech.network.endpoints

import com.spectech.domain.model.CreateEquipmentRequest
import com.spectech.domain.model.UpdateEquipmentRequest
import com.spectech.network.http.ApiTarget
import com.spectech.network.http.HttpMethod
import com.spectech.network.http.SpecTechJson
import kotlinx.serialization.encodeToString

/**
 * `/equipment` endpoint family — fetch, create, update, delete. Photo upload
 * is handled outside [ApiTarget] because it uses multipart/form-data (see
 * `EquipmentRepository.uploadPhoto`).
 *
 * Mirrors iOS `EquipmentAPI` (SpecTechIOS/Networking/Endpoints/EquipmentAPI.swift).
 */
sealed interface EquipmentApi : ApiTarget {

    /** All equipment owned by the current contractor (auth required). */
    data object FetchAll : EquipmentApi {
        override val path: String = "equipment"
        override val method: HttpMethod = HttpMethod.GET
        override val requiresAuth: Boolean = true
    }

    /** Create a new equipment unit. */
    data class Create(val request: CreateEquipmentRequest) : EquipmentApi {
        override val path: String = "equipment"
        override val method: HttpMethod = HttpMethod.POST
        override val requiresAuth: Boolean = true
        override val body: String = SpecTechJson.encodeToString(request)
    }

    /** Patch one or more fields on an existing unit. */
    data class Update(val id: String, val request: UpdateEquipmentRequest) : EquipmentApi {
        override val path: String = "equipment/${id.lowercase()}"
        override val method: HttpMethod = HttpMethod.PATCH
        override val requiresAuth: Boolean = true
        override val body: String = SpecTechJson.encodeToString(request)
    }

    /** Remove an equipment unit. Backend rejects if a deposit is active. */
    data class Delete(val id: String) : EquipmentApi {
        override val path: String = "equipment/${id.lowercase()}"
        override val method: HttpMethod = HttpMethod.DELETE
        override val requiresAuth: Boolean = true
    }
}
