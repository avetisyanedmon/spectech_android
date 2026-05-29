package com.spectech.features.notifications.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.spectech.uikit.theme.BrandBlue
import com.spectech.uikit.theme.DestructiveRed
import com.spectech.uikit.theme.SuccessGreen
import com.spectech.uikit.theme.WarningAmber

/**
 * Per-notification-type icon + tint. Mirrors the `iconColor` / `icon` mapping
 * in iOS `NotificationsView.NotificationRow`
 * (SpecTechIOS/Features/Notifications/NotificationsView.swift:67-92).
 *
 * Unknown / null types fall back to a neutral bell so a backend rolling out a
 * new event type still renders gracefully.
 */
data class NotificationVisual(val icon: ImageVector, val tint: Color)

fun visualsForType(type: String?): NotificationVisual = when (type) {
    "new_bid", "offer_created" ->
        NotificationVisual(Icons.Filled.Gavel, WarningAmber)
    "bid_accepted", "offer_accepted" ->
        NotificationVisual(Icons.Filled.CheckCircle, SuccessGreen)
    "offer_rejected", "bid_rejected" ->
        NotificationVisual(Icons.Filled.Cancel, DestructiveRed)
    "matching_order", "new_matching_order" ->
        NotificationVisual(Icons.Filled.Tune, BrandBlue)
    "order_status_changed" ->
        NotificationVisual(Icons.Filled.SwapHoriz, BrandBlue)
    "system", "announcement" ->
        NotificationVisual(Icons.Filled.AutoAwesome, BrandBlue)
    else ->
        NotificationVisual(Icons.Filled.Notifications, BrandBlue)
}
