package com.spectech.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * One row in the in-app notifications inbox. Persisted in DataStore, capped
 * to ~100 entries. Mirrors iOS `AppNotification`
 * (SpectechIOS/Features/Notifications/NotificationStore.swift).
 *
 * The [dedupeKey] resolves the standard backend pattern: same `notificationId`
 * means duplicate (rare), and otherwise an event identity composed of
 * `type:orderId:offerId|bidId` deduplicates resent pushes.
 */
@Serializable
data class AppNotification(
    val id: String,
    val notificationId: String? = null,
    val title: String,
    val body: String,
    val type: String? = null,
    val orderId: String? = null,
    val offerId: String? = null,
    val bidId: String? = null,
    val receivedAt: Instant = Clock.System.now(),
    val isRead: Boolean = false,
) {
    val dedupeKey: String?
        get() {
            if (!notificationId.isNullOrEmpty()) return notificationId
            val parts = listOfNotNull(type, orderId, offerId ?: bidId)
                .filter { it.isNotEmpty() }
            return parts.takeIf { it.isNotEmpty() }?.joinToString(":")
        }
}
