package com.spectech.domain.util

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Pure scheduling rules for the Create Order form. Bids must close before
 * work begins, so the bid-acceptance deadline may not be later than the
 * work start. Extracted from `CreateOrderViewModel` so the cross-field
 * validation is unit-testable without Android dependencies.
 */
object OrderSchedule {

    private const val AUTO_DEADLINE_LEAD_HOURS = 1

    /**
     * True when the bid-acceptance deadline is later than the work start —
     * the invalid state. A deadline equal to the start is allowed.
     */
    fun isDeadlineAfterStart(
        startDate: LocalDate,
        startTime: LocalTime,
        deadline: LocalDateTime,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Boolean {
        val startInstant = startDate.atTime(startTime).toInstant(timeZone)
        return deadline.toInstant(timeZone) > startInstant
    }

    /**
     * Suggested bid-acceptance deadline for the given work start: one hour
     * before the start (bids close before work begins), clamped so it never
     * lands in the past. When the start itself is sooner than one hour away
     * the deadline snaps to [now] — still no later than the start, so the
     * [isDeadlineAfterStart] constraint keeps holding. Mirrors iOS
     * `max(start.addingTimeInterval(-3600), Date())`.
     */
    fun autoDeadlineFor(
        date: LocalDate,
        time: LocalTime,
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): LocalDateTime {
        val startInstant = date.atTime(time).toInstant(timeZone)
        val auto = startInstant.minus(AUTO_DEADLINE_LEAD_HOURS, DateTimeUnit.HOUR, timeZone)
        val target = if (auto < now) now else auto
        return target.toLocalDateTime(timeZone)
    }
}
