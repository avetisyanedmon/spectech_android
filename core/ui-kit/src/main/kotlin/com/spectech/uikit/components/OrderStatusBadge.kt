package com.spectech.uikit.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spectech.domain.enums.OrderStatus

/**
 * Status pill used on order cards and details. Color table ported from iOS
 * `OrderStatusColor.swift` so users see the same visual cues across platforms.
 */
@Composable
fun OrderStatusBadge(
    status: OrderStatus,
    label: String,
    modifier: Modifier = Modifier,
) {
    val (background, foreground) = statusColors(status)
    Surface(
        modifier = modifier,
        color = background,
        contentColor = foreground,
        shape = RoundedCornerShape(percent = 50),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun statusColors(status: OrderStatus): Pair<Color, Color> = when (status) {
    OrderStatus.OPEN        -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
    OrderStatus.PENDING     -> Color(0xFFFFF3E0) to Color(0xFFE65100)
    OrderStatus.ACCEPTED    -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
    OrderStatus.IN_PROGRESS -> Color(0xFFE0F2F1) to Color(0xFF00695C)
    OrderStatus.COMPLETED   -> Color(0xFFE0E0E0) to Color(0xFF424242)
    OrderStatus.CANCELLED   -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    OrderStatus.EXPIRED     -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    OrderStatus.CLOSED      -> Color(0xFFE0E0E0) to Color(0xFF424242)
}
