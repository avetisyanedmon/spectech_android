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
import com.spectech.domain.util.OrderSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Drives the Create Order sheet. Mirrors iOS `CreateOrderViewModel`
 * (SpecTechIOS/Features/CreateOrder/CreateOrderView.swift) — including
 * cross-field validation (the bid-acceptance deadline may not be later than
 * the work start), the "Параметры техники: …"
 * options-summary prefix on `description`, and comma-or-dot decimal handling
 * on `workVolume`. The standalone "duration" control has been removed.
 *
 * Street + house are now **optional** (iOS lines 441-448 use no `*` marker)
 * — only region, city, and work volume are required.
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

    /**
     * Category-specific optional fields (boom length, attachments, etc.). The
     * UI mutates this directly; [submit] reads [CategoryOptionsState.buildSummary]
     * to prepend the human-readable Russian summary to the description.
     */
    val categoryOptions: CategoryOptionsState = CategoryOptionsState()

    // ─── Scheduling — local time zone semantics ────────────────────────────

    /**
     * Defaults to "now" (today at the current local time). Bids must close
     * before work begins, so the bid-acceptance deadline may not be later
     * than the work start.
     */
    var startDate by mutableStateOf(defaultStartDate())
    var startTime by mutableStateOf(defaultStartTime())

    /** Auto-tracks `start − 1h` whenever start changes, but the user can override. */
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

    /**
     * Replaces the raw work-volume input so the field accepts both comma and
     * dot decimal separators (matches iOS line 207 `replacingOccurrences(",", ".")`).
     */
    fun updateWorkVolume(raw: String) {
        workVolume = raw.filter { it.isDigit() || it == '.' || it == ',' }
    }

    // ─── Submission state ─────────────────────────────────────────────────

    var isSubmitting by mutableStateOf(false)
        private set
    var error by mutableStateOf<ApiError?>(null)
    var success by mutableStateOf(false)
        private set

    /**
     * Real-time validation: true when the bid-acceptance end time (the
     * bidding deadline) is later than the work start — bids must close no
     * later than the moment work begins. The UI resolves the localized
     * error message.
     */
    val isDeadlineAfterStart: Boolean
        get() = OrderSchedule.isDeadlineAfterStart(startDate, startTime, biddingDeadline)

    /**
     * Submit-enabled gate. Matches iOS `canSubmit` (CreateOrderView.swift:203-209):
     * only region, city, and a parseable positive work volume are required —
     * street and house number stay optional. Also requires valid time ranges.
     */
    val isFormValid: Boolean
        get() = region.isNotBlank() &&
                city.isNotBlank() &&
                workVolume.isNotBlank() &&
                parseVolume(workVolume) != null &&
                !isDeadlineAfterStart

    fun submit() {
        if (isSubmitting) return

        // Explicit cross-field validation with localized error codes — iOS
        // pre-flights the same set of checks before the network call.
        val trimmedRegion = region.trim()
        if (trimmedRegion.isEmpty()) {
            error = ApiError(
                code = ApiError.LocalCodes.FALLBACK_400,
                message = "Region is required.",
            )
            return
        }
        val trimmedCity = city.trim()
        if (trimmedCity.isEmpty()) {
            error = ApiError(
                code = ApiError.LocalCodes.FALLBACK_400,
                message = "City is required.",
            )
            return
        }
        val volume = parseVolume(workVolume)
        if (volume == null || volume <= 0.0) {
            error = ApiError(
                code = ApiError.LocalCodes.FALLBACK_400,
                message = "Enter a valid work volume.",
            )
            return
        }
        // Scheduling validation: the bid-acceptance deadline must not be
        // later than the work start.
        if (isDeadlineAfterStart) {
            error = ApiError(
                code = ApiError.LocalCodes.FALLBACK_400,
                message = "Bid acceptance must end no later than the work start.",
            )
            return
        }

        viewModelScope.launch {
            isSubmitting = true
            error = null
            try {
                val request = buildRequest(trimmedCity = trimmedCity, volume = volume)
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

    private fun buildRequest(trimmedCity: String, volume: Double): CreateOrderRequest {
        val tz = TimeZone.currentSystemDefault()
        val startDateTime = startDate.atTime(startTime)

        // adDuration matches iOS — it's the number of seconds between "now"
        // and the bidding deadline (the ad's live window). Kept as a server
        // hint; the backend may overwrite based on its own clock.
        val adDurationSeconds = (biddingDeadline.toInstant(tz) - Clock.System.now())
            .inWholeSeconds
            .coerceAtLeast(MIN_AD_DURATION_SECONDS)
            .toInt()

        // "region, street, house" but only the non-empty parts — street and
        // house are now optional and shouldn't introduce empty commas in the
        // joined string (e.g. "Moscow, , 5" → "Moscow, 5").
        val address = listOf(region, street, houseNumber)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")

        // iOS time format is "HH:mm" (CreateOrderView.swift:248 uses
        // `DateFormatter.orderTime` configured as `HH:mm`). Was sending
        // "HH:mm:ss" before this change.
        val timeStr = "${pad(startTime.hour)}:${pad(startTime.minute)}"

        // Prepend the human-readable Russian "Параметры техники: …" sentence
        // to the description so contractors see the category-specific
        // parameters inline. Marketplace card / detail screen parse the
        // prefix back out via `parseDescriptionParts` (see Section 5).
        val trimmedDescription = description.trim()
        val optionsSummary = categoryOptions.buildSummary(category)
        val combinedDescription: String? = when {
            optionsSummary == null && trimmedDescription.isEmpty() -> null
            optionsSummary == null -> trimmedDescription
            trimmedDescription.isEmpty() -> optionsSummary
            else -> optionsSummary + "\n" + trimmedDescription
        }

        return CreateOrderRequest(
            equipmentCategory = category.backendCreateValue,
            city = trimmedCity,
            street = street.trim(),
            houseNumber = houseNumber.trim(),
            address = address,
            paymentTypes = listOf(selectedPaymentType.backendCreateValue),
            pricingUnit = pricingUnit.backendCreateValue,
            workVolume = volume,
            startDate = startDate.toString(),
            startTime = timeStr,
            startDateTime = startDateTime.toInstant(tz).toString(),
            adDuration = adDurationSeconds,
            // The duration control was removed from the form; the backend still
            // requires the field, so we send a fixed default.
            durationHours = DEFAULT_DURATION_HOURS,
            expiryDateTime = biddingDeadline.toInstant(tz).toString(),
            description = combinedDescription,
        )
    }

    private fun pad(value: Int): String = value.toString().padStart(2, '0')

    /**
     * Parses the work-volume input, accepting both `.` and `,` as decimal
     * separators (Russian locale convention). Returns `null` for malformed
     * input — callers treat that as "invalid". Mirrors iOS line 207:
     * `Double(workVolume.replacingOccurrences(",", "."))`.
     */
    private fun parseVolume(raw: String): Double? =
        raw.trim().replace(',', '.').toDoubleOrNull()

    companion object {
        private const val DEFAULT_DURATION_HOURS = 8
        private const val MIN_AD_DURATION_SECONDS = 60L

        private fun defaultStartDate(): LocalDate =
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        private fun defaultStartTime(): LocalTime =
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time

        private fun defaultBiddingDeadline(): LocalDateTime =
            autoDeadlineFor(defaultStartDate(), defaultStartTime())

        /** Bidding deadline auto-tracks `start − 1h`; see [OrderSchedule.autoDeadlineFor]. */
        private fun autoDeadlineFor(date: LocalDate, time: LocalTime): LocalDateTime =
            OrderSchedule.autoDeadlineFor(date, time)
    }
}
