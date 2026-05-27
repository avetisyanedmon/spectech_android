package com.spectech.features.garage.deposit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectech.data.deposit.DepositRepository
import com.spectech.domain.enums.DepositStatus
import com.spectech.domain.error.ApiError
import com.spectech.domain.model.Deposit
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Powers the deposit info sheet for one equipment unit. Mirrors iOS
 * `DepositInfoViewModel` (SpecTechIOS/Scene/Tabs/Garage/Deposit/DepositInfoSheet.swift).
 *
 *   - On open, fetches whatever deposit (if any) exists for the equipment.
 *   - "Pay" either reuses the pending deposit's confirmation URL (idempotent
 *     on the server side) or creates a fresh one. The URL is published as
 *     [pendingConfirmationUrl] — the screen launches it via Chrome Custom Tabs
 *     and consumes the value.
 *   - [syncOnReturn] is invoked by the screen when the user comes back from
 *     the Custom Tab so the status flips from pending → paid quickly even if
 *     the YooKassa webhook hasn't arrived yet.
 *   - "Refund" initiates a refund on a paid deposit and writes back the new
 *     status.
 *
 * Assisted-injected with the equipment id so the same VM type can serve any
 * equipment row.
 */
@HiltViewModel(assistedFactory = DepositInfoViewModel.Factory::class)
class DepositInfoViewModel @AssistedInject constructor(
    @Assisted val equipmentId: String,
    private val depositRepo: DepositRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(equipmentId: String): DepositInfoViewModel
    }

    var existingDeposit by mutableStateOf<Deposit?>(null)
        private set

    /** Pending YooKassa URL the screen launches in a Custom Tab. Consumed once. */
    var pendingConfirmationUrl by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(true)
        private set
    var isPayInFlight by mutableStateOf(false)
        private set
    var isSyncInFlight by mutableStateOf(false)
        private set
    var isRefundInFlight by mutableStateOf(false)
        private set
    var error by mutableStateOf<ApiError?>(null)

    /** Convenience for the screen's "active deposit" branching. */
    val hasPaidDeposit: Boolean get() = existingDeposit?.status == DepositStatus.PAID

    fun load() {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val payload = depositRepo.depositForEquipment(equipmentId)
                existingDeposit = payload.deposit
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiError) {
                error = e
            } catch (e: Exception) {
                error = ApiError.from(e)
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Either reopens the existing pending deposit's confirmation URL or
     * creates a new deposit. The backend is idempotent on equipmentId for
     * in-flight pending payments; reusing avoids a wasted round-trip.
     */
    fun startPayment() {
        if (isPayInFlight) return
        viewModelScope.launch {
            isPayInFlight = true
            error = null
            try {
                val existing = existingDeposit
                val url = if (existing?.status == DepositStatus.PENDING &&
                    !existing.confirmationUrl.isNullOrBlank()
                ) {
                    existing.confirmationUrl
                } else {
                    val created = depositRepo.createDeposit(equipmentId)
                    existingDeposit = created
                    created.confirmationUrl
                }

                if (url.isNullOrBlank()) {
                    error = ApiError(message = "Payment URL is missing. Try again.")
                } else {
                    pendingConfirmationUrl = url
                }
            } catch (e: ApiError) {
                error = e
            } catch (e: Exception) {
                error = ApiError.from(e)
            } finally {
                isPayInFlight = false
            }
        }
    }

    fun consumePendingConfirmationUrl() {
        pendingConfirmationUrl = null
    }

    /**
     * Invoked when the user returns from the Custom Tab. Calls the sync
     * endpoint so the status flips from pending → paid before the webhook
     * arrives. No-op when there's no in-flight payment.
     */
    fun syncOnReturn() {
        val depositId = existingDeposit?.id ?: return
        val status = existingDeposit?.status ?: return
        if (status != DepositStatus.PENDING) return
        if (isSyncInFlight) return

        viewModelScope.launch {
            isSyncInFlight = true
            try {
                existingDeposit = depositRepo.syncDeposit(depositId)
            } catch (_: Exception) {
                // Best-effort — the webhook will catch up otherwise.
            } finally {
                isSyncInFlight = false
            }
        }
    }

    fun refund(reason: String? = null) {
        val depositId = existingDeposit?.id ?: return
        if (isRefundInFlight) return
        viewModelScope.launch {
            isRefundInFlight = true
            error = null
            try {
                existingDeposit = depositRepo.refundDeposit(depositId, reason)
            } catch (e: ApiError) {
                error = e
            } catch (e: Exception) {
                error = ApiError.from(e)
            } finally {
                isRefundInFlight = false
            }
        }
    }

    fun clearError() { error = null }
}
