package com.spectech.features.orders.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spectech.domain.model.Bid
import com.spectech.domain.model.Order
import com.spectech.features.orders.R
import com.spectech.uikit.components.OrderAddressLabel
import com.spectech.uikit.components.PhoneActionButton
import com.spectech.uikit.strings.label
import com.spectech.uikit.theme.SuccessGreen
import com.spectech.uikit.theme.WarningAmber
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Rich contractor-side card. Mirrors iOS `MyBidCardView`
 * (SpecTechIOS/Scene/Tabs/MyBids/Components/MyBidCardView.swift):
 *
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │  Excavator                                  [Waiting]        │
 *   │  ID: 4A2C7E13                                          ⧉    │
 *   │  📍 Moscow, Lenin Prospect 12                                │
 *   │                                                              │
 *   │  My price   Delivery                                         │
 *   │  12 000 ₽   2 500 ₽                                          │
 *   │                                                              │
 *   │  📅 Nov 15, 14:00                                            │
 *   │  ─────────────────────  (if bid accepted)                   │
 *   │  CUSTOMER CONTACTS                                           │
 *   │  👤 Ivan Petrov                                              │
 *   │  📞 +7 912 …                                                 │
 *   └─────────────────────────────────────────────────────────────┘
 *
 * Pure stateless composable — the caller passes the [Bid] and decides what
 * the status pill should read. The inline withdraw button isn't part of this
 * card; the list screen renders [WithdrawBidButton] separately under
 * non-accepted bids to match iOS' layout (`MyBidsView.swift:105-111`).
 */
@Composable
fun MyBidCard(
    order: Order,
    myBid: Bid?,
    status: MyBidStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HeaderRow(order = order, status = status)
        OrderIdRow(orderId = order.id)
        OrderAddressLabel(
            city = order.city,
            address = order.address,
            copyContentDescription = stringResource(com.spectech.uikit.R.string.action_copy_address),
        )
        myBid?.let { BidPriceRow(it) }
        order.startDateTime?.let { StartDateRow(it) }
        if (status == MyBidStatus.Accepted) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            CustomerContactCard(order = order)
        }
    }
}

/** Tri-state of the contractor's bid on a given order. */
enum class MyBidStatus { Pending, Accepted, NotSelected }

// ─── header ────────────────────────────────────────────────────────────────

@Composable
private fun HeaderRow(order: Order, status: MyBidStatus) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = order.equipmentCategory?.label() ?: "—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        StatusPill(status = status)
    }
}

@Composable
private fun StatusPill(status: MyBidStatus) {
    val (tint, labelRes) = when (status) {
        MyBidStatus.Accepted -> SuccessGreen to R.string.my_bid_status_accepted
        MyBidStatus.Pending -> WarningAmber to R.string.my_bid_status_pending
        MyBidStatus.NotSelected -> MaterialTheme.colorScheme.error to R.string.my_bid_status_rejected
    }
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = tint,
        modifier = Modifier
            .background(tint.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

// ─── order id with copy ─────────────────────────────────────────────────────

@Composable
private fun OrderIdRow(orderId: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.my_bid_order_id_label),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = orderId.take(8).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = {
                clipboard.setText(AnnotatedString(orderId.lowercase()))
                copied = true
            },
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                contentDescription = stringResource(R.string.my_bid_copy_order_id),
                tint = if (copied) SuccessGreen else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ─── price row ─────────────────────────────────────────────────────────────

@Composable
private fun BidPriceRow(bid: Bid) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        PriceColumn(
            label = stringResource(R.string.my_bid_my_price),
            value = formatCurrency(bid.price.toDouble()),
        )
        val delivery = bid.deliveryPrice
        if (delivery != null && delivery > BigDecimal.ZERO) {
            PriceColumn(
                label = stringResource(R.string.my_bid_delivery),
                value = formatCurrency(delivery.toDouble()),
            )
        }
    }
}

@Composable
private fun PriceColumn(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// ─── start date row ─────────────────────────────────────────────────────────

@Composable
private fun StartDateRow(startInstant: kotlinx.datetime.Instant) {
    val formatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = formatter.format(Date(startInstant.toEpochMilliseconds())),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── customer contact card (on accepted) ───────────────────────────────────

@Composable
private fun CustomerContactCard(order: Order) {
    val brandBlue = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(brandBlue.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.my_bid_customer_contacts),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        order.creatorName?.takeIf { it.isNotBlank() }?.let { name ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(text = name, style = MaterialTheme.typography.bodyMedium)
            }
        }
        val phone = order.creatorPhone?.takeIf { it.isNotBlank() }
        if (phone != null) {
            PhoneActionButton(
                phone = phone,
                callContentDescription = stringResource(com.spectech.uikit.R.string.state_action_call),
                copyContentDescription = stringResource(com.spectech.uikit.R.string.state_action_copy),
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(R.string.my_bid_contact_pending),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─── helpers ───────────────────────────────────────────────────────────────

private fun formatCurrency(value: Double): String {
    val fmt = NumberFormat.getCurrencyInstance(Locale("ru", "RU")).apply {
        maximumFractionDigits = 0
    }
    return runCatching { fmt.format(value) }.getOrDefault("$value ₽")
}
