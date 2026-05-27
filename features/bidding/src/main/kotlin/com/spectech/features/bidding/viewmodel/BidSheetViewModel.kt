package com.spectech.features.bidding.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectech.data.auth.SessionStore
import com.spectech.data.equipment.EquipmentRepository
import com.spectech.data.orders.OrdersRepository
import com.spectech.domain.enums.PaymentType
import com.spectech.domain.error.ApiError
import com.spectech.domain.model.CreateBidRequest
import com.spectech.domain.model.Equipment
import com.spectech.domain.model.Order
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Powers the Bid Sheet. Mirrors iOS `BidSheetViewModel`
 * (SpecTechIOS/Features/Bidding/BidSheetView.swift).
 *
 * Assisted-injected with the target [order] so the same VM type can serve any
 * order; Hilt + hilt-navigation-compose 1.2+ supply the [Factory] call site
 * via `hiltViewModel(creationCallback = …)`.
 *
 * 402 / `DEPOSIT_REQUIRED` from the backend is preserved as-is on [error];
 * the screen renders a localized deposit-required CTA when it sees that code.
 */
@HiltViewModel(assistedFactory = BidSheetViewModel.Factory::class)
class BidSheetViewModel @AssistedInject constructor(
    @Assisted val order: Order,
    private val ordersRepo: OrdersRepository,
    private val equipmentRepo: EquipmentRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(order: Order): BidSheetViewModel
    }

    // Form state
    var availableEquipment by mutableStateOf<List<Equipment>>(emptyList())
        private set
    var selectedEquipment by mutableStateOf<Equipment?>(null)
    var price by mutableStateOf("")
    var deliveryPrice by mutableStateOf("")
    var paymentType by mutableStateOf<PaymentType?>(null)
    var comment by mutableStateOf("")

    // Submit state
    var isSubmitting by mutableStateOf(false)
        private set
    var success by mutableStateOf(false)
        private set
    var error by mutableStateOf<ApiError?>(null)

    /** True only when every required field is set; lets the screen disable submit. */
    val canSubmit: Boolean
        get() = selectedEquipment != null &&
                price.toBigDecimalOrNull() != null &&
                paymentType != null &&
                !isSubmitting

    /** Whether the contractor has no equipment matching the order's category. */
    val needsEquipmentSetup: Boolean
        get() = availableEquipment.isEmpty() && !isLoading

    private var isLoading: Boolean = false

    fun load() {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val all = equipmentRepo.fetchEquipment()
                val cat = order.equipmentCategory
                val filtered = if (cat != null) all.filter { it.category == cat } else all
                availableEquipment = filtered
                if (selectedEquipment == null) selectedEquipment = filtered.firstOrNull()
                if (paymentType == null) paymentType = order.paymentTypes.firstOrNull()
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
     * Re-fetch equipment after the user adds one via the inline garage CTA.
     * The newly created item has the most recent createdAt; auto-select it.
     */
    fun reloadEquipmentAfterAdd() {
        viewModelScope.launch {
            try {
                val all = equipmentRepo.fetchEquipment()
                val cat = order.equipmentCategory
                val filtered = if (cat != null) all.filter { it.category == cat } else all
                availableEquipment = filtered
                if (selectedEquipment == null) {
                    selectedEquipment = filtered.lastOrNull() ?: filtered.firstOrNull()
                }
            } catch (_: Exception) {
                // Silent — user can retry by closing/reopening the sheet.
            }
        }
    }

    fun submit() {
        if (isSubmitting) return

        val user = sessionStore.currentUser ?: run {
            error = ApiError.MissingSession
            return
        }
        if (order.creatorId?.lowercase() == user.id.lowercase()) {
            error = ApiError(code = OWN_ORDER_CODE, message = "Cannot bid on your own order.")
            return
        }
        val equipment = selectedEquipment ?: return
        val priceDec = price.toBigDecimalOrNull() ?: run {
            error = ApiError(code = INVALID_PRICE_CODE, message = "Enter a valid price.")
            return
        }
        val deliveryDec = deliveryPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val pmt = paymentType?.takeIf { it in order.paymentTypes } ?: run {
            error = ApiError(code = INVALID_PAYMENT_CODE, message = "Pick a payment type the order accepts.")
            return
        }

        viewModelScope.launch {
            isSubmitting = true
            error = null
            try {
                val request = CreateBidRequest(
                    price = priceDec,
                    deliveryPrice = deliveryDec,
                    paymentType = pmt.backendCreateValue,
                    comment = comment.trim(),
                    equipmentId = equipment.id,
                    equipmentName = equipment.name,
                    equipmentCategory = equipment.category?.backendCreateValue.orEmpty(),
                    equipmentPhotos = equipment.photos,
                    equipmentCharacteristics = equipment.characteristics,
                    equipmentAdditionalInfo = equipment.additionalEquipment.orEmpty(),
                    contractorPhone = user.phone,
                    contractorName = user.name.orEmpty(),
                )
                ordersRepo.submitBid(order.id, request)
                success = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiError) {
                error = e
            } catch (e: Exception) {
                error = ApiError.from(e)
            } finally {
                isSubmitting = false
            }
        }
    }

    fun reset() {
        success = false
        error = null
    }

    companion object {
        const val DEPOSIT_REQUIRED_CODE = "DEPOSIT_REQUIRED"
        const val DEPOSIT_REQUIRED_STATUS = 402

        // Internal codes for client-side validation errors; the screen
        // matches on these to render the right localized message.
        const val OWN_ORDER_CODE = "CLIENT_OWN_ORDER"
        const val INVALID_PRICE_CODE = "CLIENT_INVALID_PRICE"
        const val INVALID_PAYMENT_CODE = "CLIENT_INVALID_PAYMENT"
    }
}
