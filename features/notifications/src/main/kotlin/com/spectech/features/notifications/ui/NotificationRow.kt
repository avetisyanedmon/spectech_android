package com.spectech.features.notifications.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spectech.domain.model.AppNotification
import com.spectech.uikit.strings.relativeFromNow

/**
 * One inbox row. Mirrors iOS `NotificationRow`
 * (SpecTechIOS/Features/Notifications/NotificationsView.swift:65-131):
 *
 *   ┌──────────────────────────────────────────────────────────┐
 *   │  [icon]  Title text                              [• new] │
 *   │          Body, up to 3 lines, secondary colour           │
 *   │          2 hours ago                                     │
 *   └──────────────────────────────────────────────────────────┘
 *
 * Auto-mark-as-read on first appearance (mirrors iOS `.onAppear`) — keeps the
 * unread badge truthful even if the user scrolls past without tapping.
 */
@Composable
internal fun NotificationRow(
    entry: AppNotification,
    onTap: (AppNotification) -> Unit,
    onMarkRead: (AppNotification) -> Unit,
) {
    LaunchedEffect(entry.id) {
        if (!entry.isRead) onMarkRead(entry)
    }

    val visual = visualsForType(entry.type)
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap(entry) }
            .padding(horizontal = 20.dp, vertical = 12.dp)
            // Read rows fade slightly — same 0.8 alpha iOS uses.
            .alpha(if (entry.isRead) 0.8f else 1f),
        verticalAlignment = Alignment.Top,
    ) {
        // Icon-in-tinted-circle, 40dp diameter (matches iOS layout).
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(visual.tint.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = visual.icon,
                contentDescription = null,
                tint = visual.tint,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (entry.isRead) FontWeight.Medium else FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (!entry.isRead) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape,
                            ),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = entry.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = entry.receivedAt.relativeFromNow(context),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}
