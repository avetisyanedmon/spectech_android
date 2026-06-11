package com.spectech.features.marketplace.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spectech.domain.model.Bid
import com.spectech.domain.model.Order
import com.spectech.features.marketplace.R
import com.spectech.features.marketplace.util.bidTimeRemaining
import com.spectech.uikit.components.OrderAddressLabel
import com.spectech.uikit.components.OrderStatusBadge
import com.spectech.uikit.components.PhoneActionButton
import com.spectech.uikit.strings.label
import com.spectech.uikit.theme.SuccessGreen
import com.spectech.uikit.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * One row in the marketplace list. Mirrors iOS `OrderCardView`
 * (SpecTechIOS/Scene/Tabs/Marketplace/Order/OrderCardView.swift).
 *
 *   ┌─────────────────────────────────────────────────────────────────┐
 *   │  Excavator                                          [● Open]    │
 *   │  Boom length: 12 m              (parsed from description)        │
 *   │  📍 Moscow, Lenin Prospect 12                            ⧉      │
 *   │  📊 per hour      🧊 Volume: 50                                  │
 *   │  Need experienced operator…     (free-form description body)    │
 *   │  📅 Nov 15, 14:00                                                │
 *   │  ⏲ 2 d 3 h left for bidding                                     │
 *   │  ↘  4 bids                                  [Submit price]      │
 *   └─────────────────────────────────────────────────────────────────┘
 *
 * Stateless — the caller computes role-dependent params from the parent VM's
 * cached state. iOS has these as separate @State props on the view; we keep
 * the same separation so the card stays previewable.
 *
 * @param order         the order snapshot to render
 * @param onClick       row tap — opens the order detail
 * @param onSubmitBid   pressed on the "Submit price" capsule; `null` disables
 *                      the affordance entirely (e.g. on own orders or in
 *                      unauthenticated read-only mode)
 * @param currentUserId the active session's user id; drives the
 *                      "Offer accepted" badge when the user's own bid wins
 * @param isOwnOrder    hides the contractor-side bid affordances when true
 * @param isExpired     greys out the Submit-price button past the deadline
 * @param hasSubmittedBid renders the orange "Waiting for answer" pill when
 *                      the current user already has an active bid
 * @param showBidCount  hides the bid count indicator when false (e.g. in
 *                      tabs where the count is rendered elsewhere)
 */
@Composable
fun OrderCardView(
    order: Order,
    onClick: () -> Unit,
    onSubmitBid: (() -> Unit)? = null,
    currentUserId: String? = null,
    isOwnOrder: Boolean = false,
    isExpired: Boolean = order.isExpired,
    hasSubmittedBid: Boolean = false,
    showBidCount: Boolean = true,
    showOrderId: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val parts = remember(order.description) { parseDescriptionParts(order.description) }
    val isMyBidAccepted = remember(order, currentUserId) {
        computeMyBidAccepted(order, currentUserId)
    }
    val acceptedBids = remember(order) { resolveAcceptedBids(order) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HeaderRow(order = order, parameters = parts.parameters)
            if (showOrderId) {
                OrderIdRow(orderId = order.id)
            }
            OrderAddressLabel(
                city = order.city,
                address = order.address,
                copyContentDescription = stringResource(com.spectech.uikit.R.string.action_copy_address),
                modifier = Modifier.fillMaxWidth(),
            )
            PricingChips(order = order)
            DescriptionBody(body = parts.body)
            StartDateRow(order = order)
            BidDeadlineRow(order = order)
            FooterRow(
                order = order,
                isOwnOrder = isOwnOrder,
                isExpired = isExpired,
                isMyBidAccepted = isMyBidAccepted,
                hasSubmittedBid = hasSubmittedBid,
                showBidCount = showBidCount,
                onSubmitBid = onSubmitBid,
            )

            // Contractor contact reveal — shown to the order creator (My Orders
            // tab) when at least one bid has been accepted, so the phone number
            // is reachable directly on the card without opening the detail.
            // Mirrors iOS OrderCardView.swift lines 366-396.
            if (isOwnOrder && acceptedBids.isNotEmpty()) {
                HorizontalDivider()
                acceptedBids.forEach { bid -> AcceptedContractorCard(bid = bid) }
            }
        }
    }
}

// ─── header ────────────────────────────────────────────────────────────────

@Composable
private fun HeaderRow(order: Order, parameters: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        val context = LocalContext.current
        val titleText = order.equipmentCategory?.label(context)
            ?: stringResource(R.string.order_card_equipment_fallback)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            // Customer-provided equipment parameters rendered in the same
            // font as the category so the header reads as one unit — matches
            // iOS lines 127-132 in OrderCardView.swift.
            if (!parameters.isNullOrBlank()) {
                Text(
                    text = parameters,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        OrderStatusBadge(
            status = order.status,
            label = order.status.label(),
        )
    }
}

@Composable
private fun PricingChips(order: Order) {
    if (order.pricingUnit == null && order.workVolume == null) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        order.pricingUnit?.let { unit ->
            ChipLabel(icon = Icons.Outlined.BarChart, text = unit.label())
        }
        order.workVolume?.let { vol ->
            val formatted = if (vol % 1.0 == 0.0) vol.toInt().toString() else vol.toString()
            ChipLabel(
                icon = Icons.Outlined.ViewInAr,
                text = stringResource(R.string.order_card_volume, formatted),
            )
        }
    }
}

@Composable
private fun ChipLabel(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DescriptionBody(body: String?) {
    if (body.isNullOrBlank()) return
    Text(
        text = body,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun StartDateRow(order: Order) {
    val start = order.startDateTime ?: return
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
            text = formatter.format(Date(start.toEpochMilliseconds())),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun BidDeadlineRow(order: Order) {
    val deadline = order.effectiveExpiry ?: return
    val context = LocalContext.current
    val remaining = deadline.bidTimeRemaining(context) ?: return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Timer,
            contentDescription = null,
            tint = WarningAmber,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = remaining,
            style = MaterialTheme.typography.bodySmall,
            color = WarningAmber,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun FooterRow(
    order: Order,
    isOwnOrder: Boolean,
    isExpired: Boolean,
    isMyBidAccepted: Boolean,
    hasSubmittedBid: Boolean,
    showBidCount: Boolean,
    onSubmitBid: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (showBidCount) {
            BidCountIndicator(count = order.bidCount)
        } else {
            Spacer(Modifier.height(1.dp))
        }

        if (!isOwnOrder) {
            when {
                isMyBidAccepted -> StatusPill(
                    label = stringResource(R.string.order_card_offer_accepted),
                    tint = SuccessGreen,
                )
                hasSubmittedBid -> StatusPill(
                    label = stringResource(R.string.order_card_waiting_for_answer),
                    tint = WarningAmber,
                )
                onSubmitBid != null -> SubmitPriceButton(
                    isExpired = isExpired,
                    onClick = onSubmitBid,
                )
                else -> DisabledSubmitPill()
            }
        }
    }
}

@Composable
private fun BidCountIndicator(count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.TrendingDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(R.string.order_card_bid_count, count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusPill(label: String, tint: Color) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = tint,
        modifier = Modifier
            .background(tint.copy(alpha = 0.12f), shape = CircleShape)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    )
}

@Composable
private fun SubmitPriceButton(isExpired: Boolean, onClick: () -> Unit) {
    val brandBlue = MaterialTheme.colorScheme.primary
    Text(
        text = stringResource(R.string.order_card_submit_price),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        modifier = Modifier
            .background(
                color = if (isExpired) brandBlue.copy(alpha = 0.5f) else brandBlue,
                shape = CircleShape,
            )
            .clickable(enabled = !isExpired) { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
    )
}

@Composable
private fun DisabledSubmitPill() {
    Text(
        text = stringResource(R.string.order_card_submit_price),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    )
}

// ─── parsing ───────────────────────────────────────────────────────────────

/**
 * Splits the raw description into the equipment-parameters prefix that
 * CreateOrderView prepends (e.g. "Параметры техники: длина стрелы: 12 м.\n…")
 * and the free-form customer description that follows it.
 *
 * Mirrors iOS `descriptionParts` in OrderCardView.swift (lines 66-94).
 */
private data class DescriptionParts(val parameters: String?, val body: String?)

private fun parseDescriptionParts(raw: String?): DescriptionParts {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return DescriptionParts(null, null)

    val prefix = "Параметры техники:"
    if (!trimmed.startsWith(prefix)) return DescriptionParts(null, trimmed)

    val afterPrefix = trimmed.substring(prefix.length)
    // CreateOrderViewModel.submit() separates the params line from the body
    // with a literal "\n" — match the same split-on-first-newline semantics.
    val lines = afterPrefix.split("\n", limit = 2)

    var params = lines.first().trim()
    // Builder appends a trailing period — strip it so the inline header reads
    // cleanly without an orphan dot at the end.
    if (params.endsWith(".")) params = params.dropLast(1)
    val paramsValue = params.takeIf { it.isNotEmpty() }

    val body = lines.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
    return DescriptionParts(paramsValue, body)
}

private fun computeMyBidAccepted(order: Order, currentUserId: String?): Boolean {
    val uid = currentUserId?.lowercase() ?: return false
    val ownAccepted = order.bids.firstOrNull { bid ->
        (bid.contractorId?.lowercase() == uid || bid.userId?.lowercase() == uid) && bid.isAccepted
    }
    if (ownAccepted != null) return true

    val legacy = order.acceptedBidId ?: return false
    val legacyBid = order.bids.firstOrNull { it.id == legacy } ?: return false
    return legacyBid.contractorId?.lowercase() == uid || legacyBid.userId?.lowercase() == uid
}

/**
 * Returns the bids that have been accepted on this order. Honours both the
 * `isAccepted` flag on each bid (current contract) and the legacy
 * `acceptedBidId` pointer (older backend responses). Mirrors iOS
 * `OrderCardView.acceptedBids`.
 */
private fun resolveAcceptedBids(order: Order): List<Bid> {
    val accepted = order.bids.filter { it.isAccepted }
    if (accepted.isNotEmpty()) return accepted
    val legacy = order.acceptedBidId ?: return emptyList()
    return order.bids.firstOrNull { it.id == legacy }?.let { listOf(it) } ?: emptyList()
}

// ─── order id row (My Orders / My Bids only) ───────────────────────────────

/**
 * "ID: ABC12345 ⧉" row matching iOS lines 156-182. Renders the first 8 chars
 * of the order's UUID in monospace + a copy button that places the FULL UUID
 * on the clipboard (so the user can paste it into a support chat).
 */
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
    val bumpScale by animateFloatAsState(
        targetValue = if (copied) 1.1f else 1f,
        label = "order-id-copy-bump",
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.order_card_id_label),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = orderId.take(8).uppercase(),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = {
                clipboard.setText(AnnotatedString(orderId))
                copied = true
            },
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                contentDescription = stringResource(R.string.detail_copy_order_id),
                tint = if (copied) SuccessGreen else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size((16f * bumpScale).dp),
            )
        }
    }
}

// ─── accepted contractor card (My Orders) ──────────────────────────────────

/**
 * Tinted card surfaced on the My Orders feed after the customer accepts a
 * contractor's bid: name + phone (with call/copy actions) or a "pending"
 * fallback while the backend reveals the contact. Mirrors iOS lines 366-396.
 */
@Composable
private fun AcceptedContractorCard(bid: Bid) {
    val brandBlue = MaterialTheme.colorScheme.primary
    val contact = bid.contractorContact
    val displayName = contact?.name ?: bid.contractorName
    val displayPhone = contact?.phone ?: bid.contractorPhone

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brandBlue.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.order_card_contractor_contacts),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!displayName.isNullOrBlank()) {
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
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (!displayPhone.isNullOrBlank()) {
                PhoneActionButton(
                    phone = displayPhone,
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
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.order_card_contact_not_yet_available),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
