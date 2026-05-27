package com.spectech.features.createOrder.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectech.data.orders.OrdersRepository
import com.spectech.domain.enums.EquipmentCategory
import com.spectech.domain.enums.PaymentType
import com.spectech.domain.enums.PricingUnit
import com.spectech.domain.error.ApiError
import com.spectech.domain.model.CreateOrderRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Drives the Create Order sheet. Phase 6 minimum — the 13 always-required
 * fields. Category-specific optional fields (boom length, lift capacity, etc.)
 * land in a follow-up pass; the `buildOptionsSummary()` port lives there too.
 *
 * Mirrors iOS `CreateOrderViewModel` in SpecTechIOS/Features/CreateOrder/CreateOrderView.swift.
 */
@HiltViewModel
class CreateOrderViewModel @Inject constructor(
    private val ordersRepo: OrdersRepository,
) : ViewModel() {

    // ─── Required fields ───────────────────────────────────────────────────

    var category by mutableStateOf(EquipmentCategory.DUMP_TRUCK)
    var region by mutableStateOf("")
    var city by mutableStateOf("")
    var street by mutableStateOf("")
    var houseNumber by mutableStateOf("")
    var pricingUnit by mutableStateOf(PricingUnit.PER_HOUR)
    var selectedPaymentType by mutableStateOf(PaymentType.CASH)
    var workVolume by mutableStateOf("")
    var description by mutableStateOf("")
    var durationHours by mutableStateOf(8)

    // ─── Scheduling — local time zone semantics ────────────────────────────

    /** Defaults to tomorrow at 09:00 local time. */
    var startDate by mutableStateOf(defaultStartDate())
    var startTime by mutableStateOf(LocalTime(9, 0))

    /** Auto-tracks `start - 1h` whenever start changes, but the user can override. */
    var biddingDeadline by mutableStateOf(defaultBiddingDeadline())
        private set
    private var biddingDeadlineManuallyEdited: Boolean = false

    fun updateStartDate(date: LocalDate) {
        startDate = date
        if (!biddingDeadlineManuallyEdited) biddingDeadline = autoDeadlineFor(date, startTime)
    }

    fun updateStartTime(time: LocalTime) {
        startTime = time
        if (!biddingDeadlineManuallyEdited) biddingDeadline = autoDeadlineFor(startDate, time)
    }

    fun updateBiddingDeadline(value: LocalDateTime) {
        biddingDeadline = value
        biddingDeadlineManuallyEdited = true
    }

    fun incrementDuration() { durationHours = (durationHours + 1).coerceAtMost(MAX_DURATION_HOURS) }
    fun decrementDuration() { durationHours = (durationHours - 1).coerceAtLeast(MIN_DURATION_HOURS) }

    // ─── Submission state ─────────────────────────────────────────────────

    var isSubmitting by mutableStateOf(false)
        private set
    var error by mutableStateOf<ApiError?>(null)
    var success by mutableStateOf(false)
        private set

    val isFormValid: Boolean
        get() = region.isNotBlank() &&
                city.isNotBlank() &&
                street.isNotBlank() &&
                houseNumber.isNotBlank() &&
                (workVolume.toDoubleOrNull() ?: 0.0) > 0

    fun submit() {
        if (!isFormValid || isSubmitting) return
        viewModelScope.launch {
            isSubmitting = true
            error = null
            try {
                val request = buildRequest()
                ordersRepo.createOrder(request)
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

    // ─── Wire shape construction ───────────────────────────────────────────

    private fun buildRequest(): CreateOrderRequest {
        val tz = TimeZone.currentSystemDefault()
        val startDateTime = startDate.atTime(startTime)
        val expiry = biddingDeadline

        val adDurationSeconds = (expiry.toInstant(tz) - Clock.System.now())
            .inWholeSeconds
            .coerceAtLeast(MIN_AD_DURATION_SECONDS)
            .toInt()

        val address = listOf(region, street, houseNumber)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")

        return CreateOrderRequest(
            equipmentCategory = category.backendCreateValue,
            city = city.trim(),
            street = street.trim(),
            houseNumber = houseNumber.trim(),
            address = address,
            paymentTypes = listOf(selectedPaymentType.backendCreateValue),
            pricingUnit = pricingUnit.backendCreateValue,
            workVolume = workVolume.toDouble(),
            startDate = startDate.toString(),
            startTime = "${pad(startTime.hour)}:${pad(startTime.minute)}:00",
            startDateTime = startDateTime.toInstant(tz).toString(),
            adDuration = adDurationSeconds,
            durationHours = durationHours,
            expiryDateTime = expiry.toInstant(tz).toString(),
            description = description.trim().ifEmpty { null },
        )
    }

    private fun pad(value: Int): String = value.toString().padStart(2, '0')

    companion object {
        private const val MIN_DURATION_HOURS = 1
        private const val MAX_DURATION_HOURS = 24 * 30
        private const val MIN_AD_DURATION_SECONDS = 60L

        private fun defaultStartDate(): LocalDate {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            return now.date.plus(1, DateTimeUnit.DAY)
        }

        private fun defaultBiddingDeadline(): LocalDateTime =
            autoDeadlineFor(defaultStartDate(), LocalTime(9, 0))

        private fun autoDeadlineFor(date: LocalDate, time: LocalTime): LocalDateTime {
            val tz = TimeZone.currentSystemDefault()
            val startInstant = date.atTime(time).toInstant(tz)
            val auto = startInstant.minus(1, DateTimeUnit.HOUR, tz)
            val now = Clock.System.now()
            val target = if (auto < now) now else auto
            return target.toLocalDateTime(tz)
        }
    }
}
