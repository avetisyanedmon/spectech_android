package com.spectech.features.notifications.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spectech.domain.model.AppNotification
import com.spectech.features.notifications.R
import com.spectech.uikit.components.EmptyStateView

/**
 * In-app notifications inbox. Mirrors iOS `NotificationsView`
 * (SpecTechIOS/Features/Notifications/NotificationsView.swift).
 *
 *   ┌──────────────────────────────────────────────────────────┐
 *   │  Notifications     Mark all read     Clear all           │
 *   ├──────────────────────────────────────────────────────────┤
 *   │  [🎯] New bid                                       •    │
 *   │       Ivan submitted a bid on your dump-truck order      │
 *   │       2 hours ago                                        │
 *   ├──────────────────────────────────────────────────────────┤
 *   │  [✓] Bid accepted                                        │
 *   │       Your bid won on the excavator rental               │
 *   │       Yesterday                                          │
 *   └──────────────────────────────────────────────────────────┘
 *
 * The composable is **stateless** — the host (currently
 * `app/.../ActionSheets.NotificationsSheet`) is responsible for wiring the
 * [NotificationStore] flow + the deep-link request callback. That separation
 * keeps `:features:notifications` free of DI machinery and reusable from a
 * future standalone screen / tablet sidebar.
 *
 * @param notifications snapshot of [com.spectech.data.notifications.NotificationStore]'s
 *   list flow. The caller is responsible for empty-vs-loaded routing — this
 *   function renders an empty state internally when the list is empty.
 * @param onTap fired on row click. Caller marks-read, requests deep-link, dismisses.
 * @param onMarkRead fired on first appearance of an unread row. Mirrors iOS' onAppear.
 * @param onMarkAllRead toolbar action — bulk-flip all rows to read.
 * @param onClearAll toolbar action — drop the entire inbox.
 */
@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onTap: (AppNotification) -> Unit,
    onMarkRead: (AppNotification) -> Unit,
    onMarkAllRead: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (notifications.isEmpty()) {
        EmptyStateView(
            title = stringResource(R.string.notifications_empty_title),
            message = stringResource(R.string.notifications_empty_message),
            icon = Icons.Outlined.NotificationsOff,
            paddingValues = PaddingValues(0.dp),
            modifier = modifier,
        )
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ToolbarRow(
            onMarkAllRead = onMarkAllRead,
            onClearAll = onClearAll,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(items = notifications, key = { it.id }) { entry ->
                NotificationRow(
                    entry = entry,
                    onTap = onTap,
                    onMarkRead = onMarkRead,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun ToolbarRow(
    onMarkAllRead: () -> Unit,
    onClearAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.notifications_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onMarkAllRead) {
                Text(stringResource(R.string.notifications_mark_all_read))
            }
            TextButton(onClick = onClearAll) {
                Text(
                    text = stringResource(R.string.notifications_clear_all),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
    // Placeholder so the Column doesn't collapse when the LazyColumn below
    // ends up empty mid-animation.
    Box(modifier = Modifier.fillMaxWidth())
}
