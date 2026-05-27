package com.spectech.network.endpoints

import com.spectech.domain.model.CreateDepositRequest
import com.spectech.domain.model.RefundDepositRequest
import com.spectech.network.http.ApiTarget
import com.spectech.network.http.HttpMethod
import com.spectech.network.http.SpecTechJson
import kotlinx.serialization.encodeToString

/**
 * `/payments/deposits` endpoint family for the YooKassa-backed performance
 * security deposit flow. Mirrors iOS `DepositAPI` in
 * SpecTechIOS/Networking/Endpoints/DepositAPI.swift.
 */
sealed interface DepositApi : ApiTarget {

    /**
     * Creates a new pending deposit for the given equipment unit. Returned
     * [com.spectech.domain.model.Deposit] carries the YooKassa-hosted
     * `confirmationUrl` the contractor must open to finish payment.
     */
    data class Create(val request: CreateDepositRequest) : DepositApi {
        override val path: String = "payments/deposits"
        override val method: HttpMethod = HttpMethod.POST
        override val requiresAuth: Boolean = true
        override val body: String = SpecTechJson.encodeToString(request)
    }

    /** Returns the active deposit attached to an equipment unit (or null). */
    data class GetForEquipment(val equipmentId: String) : DepositApi {
        override val path: String = "payments/deposits/equipment/${equipmentId.lowercase()}"
        override val method: HttpMethod = HttpMethod.GET
        override val requiresAuth: Boolean = true
    }

    /**
     * Asks the backend to refresh the deposit's status from YooKassa. Used
     * after the user dismisses the Custom Tab so the UI can flip from
     * pending → paid before the webhook arrives.
     */
    data class Sync(val depositId: String) : DepositApi {
        override val path: String = "payments/deposits/${depositId.lowercase()}/sync"
        override val method: HttpMethod = HttpMethod.POST
        override val requiresAuth: Boolean = true
    }

    /** Initiates a refund of a paid deposit. */
    data class Refund(val depositId: String, val request: RefundDepositRequest) : DepositApi {
        override val path: String = "payments/deposits/${depositId.lowercase()}/refund"
        override val method: HttpMethod = HttpMethod.POST
        override val requiresAuth: Boolean = true
        override val body: String = SpecTechJson.encodeToString(request)
    }
}
