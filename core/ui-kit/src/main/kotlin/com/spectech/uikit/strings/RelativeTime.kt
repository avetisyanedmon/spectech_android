package com.spectech.uikit.strings

import android.content.Context
import com.spectech.uikit.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * "just now" / "2 min ago" / "3 hr ago" / "5 days ago" / "12 May 2026".
 * Mirrors iOS `RelativeDateTimeFormatter` output but expressed via Android
 * plural resources so the Russian translation gets correct one/few/many forms.
 *
 * Anything older than 7 days falls back to absolute formatting in the device
 * locale, which matches what iOS does for older entries.
 *
 * Shared across News (post timestamps) and Notifications (received-at) so a
 * future caller doesn't need to duplicate the resource set.
 */
fun Instant.relativeFromNow(context: Context): String {
    val now = Clock.System.now()
    val diff = now - this
    return when {
        diff < 1.minutes -> context.getString(R.string.time_just_now)
        diff < 1.hours -> {
            val mins = diff.inWholeMinutes.toInt().coerceAtLeast(1)
            context.resources.getQuantityString(R.plurals.time_min_ago, mins, mins)
        }
        diff < 1.days -> {
            val hrs = diff.inWholeHours.toInt().coerceAtLeast(1)
            context.resources.getQuantityString(R.plurals.time_hr_ago, hrs, hrs)
        }
        diff < 7.days -> {
            val days = diff.inWholeDays.toInt().coerceAtLeast(1)
            context.resources.getQuantityString(R.plurals.time_days_ago, days, days)
        }
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            .format(Date(toEpochMilliseconds()))
    }
}
