package com.spectech.domain.util

import io.kotest.matchers.shouldBe
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.junit.jupiter.api.Test

class OrderScheduleTest {

    private val tz = TimeZone.of("UTC")
    private val startDate = LocalDate(2026, 7, 20)
    private val startTime = LocalTime(10, 0)

    private fun isInvalid(deadline: LocalDateTime): Boolean =
        OrderSchedule.isDeadlineAfterStart(startDate, startTime, deadline, tz)

    @Test
    fun `deadline later than start is invalid`() {
        // Arrange
        val deadline = startDate.atTime(LocalTime(10, 1))

        // Act + Assert
        isInvalid(deadline) shouldBe true
    }

    @Test
    fun `deadline on a later date is invalid`() {
        val deadline = LocalDate(2026, 7, 21).atTime(LocalTime(9, 0))

        isInvalid(deadline) shouldBe true
    }

    @Test
    fun `deadline equal to start is allowed`() {
        val deadline = startDate.atTime(startTime)

        isInvalid(deadline) shouldBe false
    }

    @Test
    fun `deadline before start is allowed`() {
        val deadline = startDate.atTime(LocalTime(8, 30))

        isInvalid(deadline) shouldBe false
    }

    @Test
    fun `auto deadline is one hour before a distant start`() {
        // Arrange — start is far beyond one hour from "now".
        val now = Instant.parse("2026-07-19T12:00:00Z")

        // Act
        val deadline = OrderSchedule.autoDeadlineFor(startDate, startTime, now, tz)

        // Assert
        deadline shouldBe startDate.atTime(LocalTime(9, 0))
    }

    @Test
    fun `auto deadline clamps to now when start is less than an hour away`() {
        val now = Instant.parse("2026-07-20T09:30:00Z")

        val deadline = OrderSchedule.autoDeadlineFor(startDate, startTime, now, tz)

        deadline shouldBe LocalDate(2026, 7, 20).atTime(LocalTime(9, 30))
    }

    @Test
    fun `auto deadline never precedes now even for a past start`() {
        val now = Instant.parse("2026-07-20T11:00:00Z")

        val deadline = OrderSchedule.autoDeadlineFor(startDate, startTime, now, tz)

        deadline shouldBe LocalDate(2026, 7, 20).atTime(LocalTime(11, 0))
    }

    @Test
    fun `auto deadline for a future start is never after the start`() {
        val now = Instant.parse("2026-07-20T09:59:00Z")

        val deadline = OrderSchedule.autoDeadlineFor(startDate, startTime, now, tz)

        OrderSchedule.isDeadlineAfterStart(startDate, startTime, deadline, tz) shouldBe false
        deadline.toInstant(tz) shouldBe now
    }
}
