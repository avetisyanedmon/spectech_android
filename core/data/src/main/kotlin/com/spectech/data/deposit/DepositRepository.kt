package com.spectech.data.deposit

import com.spectech.data.events.AppEventBus
import com.spectech.data.events.DomainEvent
import com.spectech.domain.model.CreateDepositRequest
import com.spectech.domain.model.Deposit
import com.spectech.domain.model.DepositForEquipmentPayload
import com.spectech.domain.model.RefundDepositRequest
import com.spectech.network.endpoints.DepositApi
import com.spectech.network.http.ApiClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps the `/payments/deposits` endpoint family. Mirrors iOS `DepositService`.
 *
 * Every mutation emits [DomainEvent.EquipmentChanged] so the Garage list and
 * any open detail screen refresh — that's how the "paid" badge appears on
 * the equipment card without a manual reload.
 */
@Singleton
class DepositRepository @Inject constructor(
    private val api: ApiClient,
    private val events: AppEventBus,
) {

    /** Creates a fresh pending deposit. Returns the YooKassa confirmation URL inside the deposit. */
    suspend fun createDeposit(equipmentId: String): Deposit {
        val request = CreateDepositRequest(equipmentId = equipmentId)
        val response = api.send<Deposit>(DepositApi.Create(request)).data
        events.emit(DomainEvent.EquipmentChanged)
        return response
    }

    /** Returns the equipment's active deposit (if any) plus a server-computed `isPaid` flag. */
    suspend fun depositForEquipment(equipmentId: String): DepositForEquipmentPayload =
        api.send<DepositForEquipmentPayload>(DepositApi.GetForEquipment(equipmentId)).data

    /**
     * Re-queries YooKassa via the backend. Called after the Custom Tab closes
     * so the UI flips from pending → paid before the webhook arrives. Emits
     * [DomainEvent.EquipmentChanged] regardless of whether the status moved —
     * the listener can compare against its cache cheaply.
     */
    suspend fun syncDeposit(depositId: String): Deposit {
        val response = api.send<Deposit>(DepositApi.Sync(depositId)).data
        events.emit(DomainEvent.EquipmentChanged)
        return response
    }

    /** Initiates a refund. Status will go to `refund_pending`, then `refunded`. */
    suspend fun refundDeposit(depositId: String, reason: String? = null): Deposit {
        val response = api.send<Deposit>(DepositApi.Refund(depositId, RefundDepositRequest(reason))).data
        events.emit(DomainEvent.EquipmentChanged)
        return response
    }
}
