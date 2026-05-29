package com.spectech.features.marketplace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectech.domain.enums.OrderStatus
import com.spectech.domain.model.Order
import com.spectech.features.marketplace.R
import com.spectech.features.marketplace.ui.components.BidApplicationCard
import com.spectech.features.marketplace.viewmodel.MarketplaceViewModel
import com.spectech.features.marketplace.viewmodel.OrderDetailViewModel
import com.spectech.uikit.components.LoadingStateView
import com.spectech.uikit.components.OrderStatusBadge
import com.spectech.uikit.strings.label
import com.spectech.uikit.strings.localizedMessage
import com.spectech.uikit.theme.SuccessGreen
import com.spectech.uikit.theme.WarningAmber
import com.spectech.uikit.theme.DestructiveRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Full marketplace order detail. Role-branches the action area:
 *
 *   - **Owner (customer)**: list of received bids, per-bid Accept button, inline
 *     contact reveal on the accepted bid, Delete-order toolbar action.
 *   - **Contractor with submitted bid**: "Waiting for answer" banner + Withdraw
 *     button until the customer accepts; if accepted, the customer's contact
 *     card appears at the top.
 *   - **Contractor without a bid**: "Submit price" CTA (disabled if expired).
 *   - **Unauthenticated**: sign-in prompt + "Submit price" routes through auth.
 *
 * Mirrors iOS `OrderDetailView`
 * (SpecTechIOS/Scene/Tabs/Marketplace/Order/OrderDetailView.swift).
 */
@Composable
fun OrderDetailScreen(
    orderId: String,
    onSubmitBid: (Order) -> Unit,
    onSignIn: () -> Unit = {},
    onClose: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues(),
    marketplaceViewModel: MarketplaceViewModel,
    detailViewModel: OrderDetailViewModel = hiltViewModel(),
) {
    // Seed from cache the first time the screen composes.
    LaunchedEffect(orderId) {
        detailViewModel.hydrateFromCache(marketplaceViewModel.findOrder(orderId))
    }

    val order by detailViewModel.order.collectAsStateWithLifecycle()
    val isLoading by detailViewModel.isLoading.collectAsStateWithLifecycle()
    val loadError by detailViewModel.loadError.collectAsStateWithLifecycle()

    val current = order
    if (current == null) {
        if (loadError != null) {
            // Defer to ErrorStateView for parity with the rest of the app.
            com.spectech.uikit.components.ErrorStateView(
                error = loadError!!,
                title = stringResource(com.spectech.uikit.R.string.state_error_title),
                retryTitle = stringResource(com.spectech.uikit.R.string.state_retry),
                onRetry = { detailViewModel.refresh() },
                paddingValues = paddingValues,
            )
        } else {
            LoadingStateView(
                title = stringResource(com.spectech.uikit.R.string.state_loading),
                paddingValues = paddingValues,
            )
        }
        return
    }

    OrderDetailContent(
        order = current,
        isLoading = isLoading,
        isOwn = detailViewModel.isOwnOrder(current),
        myBid = detailViewModel.myBid(current),
        isMyBidAccepted = detailViewModel.isMyBidAccepted(current),
        acceptedBidIds = detailViewModel.acceptedBidIds.collectAsStateWithLifecycle().value,
        acceptedContacts = detailViewModel.acceptedContacts.collectAsStateWithLifecycle().value,
        acceptingBidId = detailViewModel.acceptingBidId,
        isAuthenticated = detailViewModel.isAuthenticated,
        isWithdrawing = detailViewModel.isWithdrawing,
        isDeleting = detailViewModel.isDeleting,
        paddingValues = paddingValues,
        onAccept = { bid -> detailViewModel.acceptBid(bid) },
        onSubmitBid = { onSubmitBid(current) },
        onSignIn = onSignIn,
        onRequestWithdraw = { detailViewModel.withdrawBid(onSuccess = onClose) },
        onRequestDelete = { detailViewModel.deleteOrder(onSuccess = onClose) },
    )

    // Alerts (accept / withdraw / delete failure) — modal AlertDialog matching iOS.
    detailViewModel.acceptError?.let { err ->
        AlertDialog(
            onDismissRequest = { detailViewModel.acceptError = null },
            title = { Text(stringResource(R.string.detail_alert_accept_failed)) },
            text = { Text(err.localizedMessage()) },
            confirmButton = {
                TextButton(onClick = { detailViewModel.acceptError = null }) { Text("OK") }
            },
        )
    }
    detailViewModel.withdrawError?.let { err ->
        AlertDialog(
            onDismissRequest = { detailViewModel.withdrawError = null },
            title = { Text(stringResource(R.string.detail_alert_withdraw_failed)) },
            text = { Text(err.localizedMessage()) },
            confirmButton = {
                TextButton(onClick = { detailViewModel.withdrawError = null }) { Text("OK") }
            },
        )
    }
    detailViewModel.deleteError?.let { err ->
        AlertDialog(
            onDismissRequest = { detailViewModel.deleteError = null },
            title = { Text(stringResource(R.string.detail_alert_delete_failed)) },
            text = { Text(err.localizedMessage()) },
            confirmButton = {
                TextButton(onClick = { detailViewModel.deleteError = null }) { Text("OK") }
            },
        )
    }
}

@Composable
private fun OrderDetailContent(
    order: Order,
    isLoading: Boolean,
    isOwn: Boolean,
    myBid: com.spectech.domain.model.Bid?,
    isMyBidAccepted: Boolean,
    acceptedBidIds: Set<String>,
    acceptedContacts: Map<String, com.spectech.domain.model.ContractorContact>,
    acceptingBidId: String?,
    isAuthenticated: Boolean,
    isWithdrawing: Boolean,
    isDeleting: Boolean,
    paddingValues: PaddingValues,
    onAccept: (com.spectech.domain.model.Bid) -> Unit,
    onSubmitBid: () -> Unit,
    onSignIn: () -> Unit,
    onRequestWithdraw: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    var showWithdrawConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeaderCard(order = order, isOwn = isOwn)
        DescriptionCard(order = order)

        if (isOwn && order.bids.isNotEmpty()) {
            ReceivedApplicationsSection(
                order = order,
                acceptedBidIds = acceptedBidIds,
                acceptedContacts = acceptedContacts,
                acceptingBidId = acceptingBidId,
                onAccept = onAccept,
            )
        }

        ActionArea(
            order = order,
            isOwn = isOwn,
            myBid = myBid,
            isMyBidAccepted = isMyBidAccepted,
            isAuthenticated = isAuthenticated,
            isWithdrawing = isWithdrawing,
            onSubmitBid = onSubmitBid,
            onSignIn = onSignIn,
            onWithdraw = { showWithdrawConfirm = true },
        )

        if (isOwn) {
            DeleteOrderButton(
                isDeleting = isDeleting,
                onRequest = { showDeleteConfirm = true },
            )
        }
    }

    if (showWithdrawConfirm) {
        AlertDialog(
            onDismissRequest = { showWithdrawConfirm = false },
            title = { Text(stringResource(R.string.detail_withdraw_confirm_title)) },
            text = { Text(stringResource(R.string.detail_withdraw_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWithdrawConfirm = false
                        onRequestWithdraw()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.detail_withdraw_action),
                        color = DestructiveRed,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawConfirm = false }) {
                    Text(stringResource(R.string.detail_cancel))
                }
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.detail_delete_confirm_title)) },
            text = { Text(stringResource(R.string.detail_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onRequestDelete()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.detail_delete_action),
                        color = DestructiveRed,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.detail_cancel))
                }
            },
        )
    }
}

// ─── header card ───────────────────────────────────────────────────────────

@Composable
private fun HeaderCard(order: Order, isOwn: Boolean) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Title + status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = order.equipmentCategory?.label(context)
                        ?: stringResource(R.string.detail_no_category),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                OrderStatusBadge(
                    status = order.status,
                    label = order.status.label(),
                )
            }

            OrderIdRow(orderId = order.id)
            OrderLocationRow(order = order)
            OrderStartDateRow(order = order)
            OrderDeadlineRow(order = order)
            if (isOwn) {
                OrderBidCountRow(order = order)
            }

            // Terms chips
            if (order.paymentTypes.isNotEmpty() || order.pricingUnit != null || order.workVolume != null) {
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    order.pricingUnit?.let {
                        Chip(text = it.label(), icon = Icons.Outlined.BarChart)
                    }
                    order.workVolume?.let {
                        Chip(
                            text = stringResource(
                                R.string.detail_chip_volume,
                                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString(),
                            ),
                            icon = Icons.Outlined.ViewInAr,
                        )
                    }
                    order.paymentTypes.forEach { pt ->
                        Chip(text = pt.label(context), icon = Icons.Outlined.CreditCard)
                    }
                }
            }
        }
    }
}

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

    Column {
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Numbers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = orderId.lowercase(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
            IconButton(
                onClick = {
                    clipboard.setText(AnnotatedString(orderId.lowercase()))
                    copied = true
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    contentDescription = stringResource(R.string.detail_copy_order_id),
                    tint = if (copied) SuccessGreen else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun OrderLocationRow(order: Order) {
    val clipboard = LocalClipboardManager.current
    val display = order.fullAddress
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = display,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = { clipboard.setText(AnnotatedString(display)) },
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = stringResource(R.string.detail_copy_address),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun OrderStartDateRow(order: Order) {
    val start = order.startDateTime ?: return
    val formatted = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        .format(Date(start.toEpochMilliseconds()))
    LabeledRow(icon = Icons.Outlined.CalendarToday, text = formatted)
}

@Composable
private fun OrderDeadlineRow(order: Order) {
    val deadline = order.effectiveExpiry ?: return
    val formatted = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        .format(Date(deadline.toEpochMilliseconds()))
    LabeledRow(
        icon = Icons.Outlined.AccessTime,
        text = stringResource(R.string.detail_bid_deadline, formatted),
    )
}

@Composable
private fun OrderBidCountRow(order: Order) {
    LabeledRow(
        icon = Icons.AutoMirrored.Outlined.TrendingDown,
        text = stringResource(R.string.detail_bid_count, order.bidCount),
    )
}

@Composable
private fun LabeledRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Chip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
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
            fontWeight = FontWeight.Medium,
        )
    }
}

// ─── description card ──────────────────────────────────────────────────────

@Composable
private fun DescriptionCard(order: Order) {
    val desc = order.description?.takeIf { it.isNotBlank() } ?: return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.detail_description).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = desc, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ─── received applications ─────────────────────────────────────────────────

@Composable
private fun ReceivedApplicationsSection(
    order: Order,
    acceptedBidIds: Set<String>,
    acceptedContacts: Map<String, com.spectech.domain.model.ContractorContact>,
    acceptingBidId: String?,
    onAccept: (com.spectech.domain.model.Bid) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.detail_received_applications, order.bids.size),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        order.bids.forEach { bid ->
            val accepted = acceptedBidIds.contains(bid.id) || bid.isAccepted
            BidApplicationCard(
                bid = bid,
                pricingUnit = order.pricingUnit,
                isOwnOrder = true,
                isAccepted = accepted,
                isAccepting = acceptingBidId == bid.id,
                acceptedContact = acceptedContacts[bid.id],
                onAccept = if (!accepted && acceptingBidId == null) { { onAccept(bid) } } else null,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

// ─── action area ───────────────────────────────────────────────────────────

@Composable
private fun ActionArea(
    order: Order,
    isOwn: Boolean,
    myBid: com.spectech.domain.model.Bid?,
    isMyBidAccepted: Boolean,
    isAuthenticated: Boolean,
    isWithdrawing: Boolean,
    onSubmitBid: () -> Unit,
    onSignIn: () -> Unit,
    onWithdraw: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!isOwn) {
            when {
                isMyBidAccepted -> CustomerContactCard(order)
                myBid != null -> {
                    WaitingForAnswerBanner()
                    WithdrawBidButton(
                        isWithdrawing = isWithdrawing,
                        onClick = onWithdraw,
                    )
                }
                isAuthenticated -> SubmitPriceButton(
                    isExpired = order.isExpired,
                    onClick = onSubmitBid,
                )
                else -> SubmitPriceButton(
                    isExpired = false,
                    onClick = onSignIn,
                )
            }

            // Privacy notice — hidden once a bid wins.
            if (!isMyBidAccepted) {
                PrivacyNotice()
            }
        }

        if (!isAuthenticated) {
            SignInBanner(onClick = onSignIn)
        }
    }
}

@Composable
private fun SubmitPriceButton(isExpired: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isExpired,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        ),
    ) {
        Text(
            text = stringResource(R.string.detail_submit_price),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

@Composable
private fun WaitingForAnswerBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(WarningAmber.copy(alpha = 0.12f), CircleShape),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.HourglassEmpty,
            contentDescription = null,
            tint = WarningAmber,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = stringResource(R.string.detail_waiting_for_answer),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = WarningAmber,
        )
    }
}

@Composable
private fun WithdrawBidButton(isWithdrawing: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isWithdrawing,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = DestructiveRed,
            disabledContainerColor = DestructiveRed.copy(alpha = 0.6f),
        ),
    ) {
        if (isWithdrawing) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Text(
                text = stringResource(R.string.detail_withdraw_action),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun CustomerContactCard(order: Order) {
    val brandBlue = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(brandBlue.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.detail_customer_contacts).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
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
            com.spectech.uikit.components.PhoneActionButton(
                phone = phone,
                callContentDescription = stringResource(com.spectech.uikit.R.string.state_action_call),
                copyContentDescription = stringResource(com.spectech.uikit.R.string.state_action_copy),
            )
        } else {
            Text(
                text = stringResource(R.string.detail_contact_not_yet_available),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PrivacyNotice() {
    val brandBlue = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(brandBlue.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            tint = brandBlue,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.detail_privacy_notice),
            style = MaterialTheme.typography.bodyMedium,
            color = brandBlue,
        )
    }
}

@Composable
private fun SignInBanner(onClick: () -> Unit) {
    val brandBlue = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(brandBlue.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(brandBlue.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = brandBlue,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = stringResource(R.string.detail_signin_prompt_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.detail_signin_prompt_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── delete order ──────────────────────────────────────────────────────────

@Composable
private fun DeleteOrderButton(isDeleting: Boolean, onRequest: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Button(
            onClick = onRequest,
            enabled = !isDeleting,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = DestructiveRed,
                disabledContainerColor = DestructiveRed.copy(alpha = 0.6f),
            ),
            border = androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent),
        ) {
            if (isDeleting) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.detail_delete_order),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}
