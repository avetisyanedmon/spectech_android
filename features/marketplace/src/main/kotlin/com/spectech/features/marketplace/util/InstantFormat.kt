package com.spectech.features.marketplace.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Cheap relative-date stringifier used by [OrderCardView] and bid lists.
 * Falls through to an absolute `dd MMM yyyy` once the order is more than a
 * week old. Full localized variant lands when we hook into Android's
 * `DateUtils.getRelativeTimeSpanString` or kotlinx-datetime's localization.
 */
fun Instant.relativeFromNow(): String {
    val now = Clock.System.now()
    val diff = now - this
    return when {
        diff < 1.minutes -> "just now"
        diff < 1.hours -> "${diff.inWholeMinutes} min ago"
        diff < 1.days -> "${diff.inWholeHours} hr ago"
        diff < 7.days -> "${diff.inWholeDays} d ago"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            .format(Date(toEpochMilliseconds()))
    }
}
