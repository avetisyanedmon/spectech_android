package com.spectech.domain.model

/**
 * One-shot deep-link request published by [com.spectech.data.notifications.NotificationStore].
 * The root navigator observes the latest request and routes to the matching
 * tab + order detail, then calls `finishNavigationRequest` so subsequent
 * recompositions don't re-trigger.
 */
data class NotificationNavigationRequest(
    val id: String,
    val type: String?,
    val orderId: String,
    val offerId: String? = null,
)
