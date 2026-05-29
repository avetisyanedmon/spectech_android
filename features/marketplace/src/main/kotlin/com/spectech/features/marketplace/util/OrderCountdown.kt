package com.spectech.features.marketplace.util

import android.content.Context
import com.spectech.features.marketplace.R
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * "2 d 3 h left for bidding" / "1 h 30 min left for bidding" / "15 min left
 * for bidding". Mirrors `bidTimeRemaining` in iOS
 * SpecTechIOS/Scene/Tabs/Marketplace/Order/OrderCardView.swift (lines 47-61).
 *
 * Returns `null` once the deadline has passed so callers can drop the row
 * entirely instead of rendering a negative countdown. The order detail screen
 * uses its own absolute-time label for expired deadlines.
 */
fun Instant.bidTimeRemaining(context: Context): String? {
    val now = Clock.System.now()
    if (this <= now) return null
    val totalSec = (this - now).inWholeSeconds
    val days = (totalSec / SECONDS_PER_DAY).toInt()
    val remainderAfterDays = totalSec % SECONDS_PER_DAY
    val hours = (remainderAfterDays / SECONDS_PER_HOUR).toInt()
    val remainderAfterHours = remainderAfterDays % SECONDS_PER_HOUR
    val minutesPartial = (remainderAfterHours / SECONDS_PER_MINUTE).toInt()
    val totalMinutes = (totalSec / SECONDS_PER_MINUTE).toInt()

    return when {
        days > 0 ->
            context.getString(R.string.bid_countdown_days_hours, days, hours)
        hours > 0 ->
            context.getString(R.string.bid_countdown_hours_minutes, hours, minutesPartial)
        else ->
            context.getString(R.string.bid_countdown_minutes, totalMinutes.coerceAtLeast(0))
    }
}

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 60L * SECONDS_PER_MINUTE
private const val SECONDS_PER_DAY = 24L * SECONDS_PER_HOUR
