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
 * Drives the Create Order sheet. Mirrors iOS `CreateOrderViewModel`
 * (SpecTechIOS/Features/CreateOrder/CreateOrderView.swift) — including
 * cross-field validation (bidding deadline strictly before start), the
 * "Параметры техники: …" options-summary prefix on `description`, and
 * comma-or-dot decimal handling on `workVolume`.
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
    var durationHours by mutableStateOf(8)

    /**
     * Category-specific optional fields (boom length, attachments, etc.). The
     * UI mutates this directly; [submit] reads [CategoryOptionsState.buildSummary]
     * to prepend the human-readable Russian summary to the description.
     */
    val categoryOptions: CategoryOptionsState = CategoryOptionsState()

    // ─── Scheduling — local time zone semantics ────────────────────────────

    /** Defaults to tomorrow at 09:00 local time (iOS uses "tomorrow"). */
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

    /**
     * Replaces the raw work-volume input so the field accepts both comma and
     * dot decimal separators (matches iOS line 207 `replacingOccurrences(",", ".")`).
     */
    fun updateWorkVolume(raw: String) {
        workVolume = raw.filter { it.isDigit() || it == '.' || it == ',' }
    }

    fun incrementDuration() { durationHours = (durationHours + 1).coerceAtMost(MAX_DURATION_HOURS) }
    fun decrementDuration() { durationHours = (durationHours - 1).coerceAtLeast(MIN_DURATION_HOURS) }

    // ─── Submission state ─────────────────────────────────────────────────

    var isSubmitting by mutableStateOf(false)
        private set
    var error by mutableStateOf<ApiError?>(null)
    var success by mutableStateOf(false)
        private set

    /**
     * Submit-enabled gate. Matches iOS `canSubmit` (CreateOrderView.swift:203-209):
     * only region, city, and a parseable positive work volume are required —
     * street and house number stay optional.
     */
    val isFormValid: Boolean
        get() = region.isNotBlank() &&
                city.isNotBlank() &&
                workVolume.isNotBlank() &&
                parseVolume(workVolume) != null

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
        // bidding deadline must be strictly before the start date — iOS line 237-240.
        val tz = TimeZone.currentSystemDefault()
        val startInstant = startDate.atTime(startTime).toInstant(tz)
        val deadlineInstant = biddingDeadline.toInstant(tz)
        if (deadlineInstant >= startInstant) {
            error = ApiError(
                code = ApiError.LocalCodes.FALLBACK_400,
                message = "Bidding deadline must be before the start date.",
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
            durationHours = durationHours,
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
