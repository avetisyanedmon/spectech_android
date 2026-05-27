package com.spectech.features.marketplace.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectech.domain.enums.OrderStatus
import com.spectech.domain.model.Order
import com.spectech.domain.state.RemoteState
import com.spectech.features.marketplace.R
import com.spectech.features.marketplace.util.relativeFromNow
import com.spectech.features.marketplace.viewmodel.MarketplaceViewModel
import com.spectech.uikit.components.LoadingStateView
import com.spectech.uikit.components.OrderStatusBadge
import com.spectech.uikit.strings.label

/**
 * Order detail visible from the public Marketplace. Phase 9 added the
 * "Submit a bid" CTA — tapping it fires [onSubmitBid] so the parent
 * (`:app`) can present the BidSheet at root level.
 *
 * Accept-bid affordances for the order's creator live in MyOrders (Phase 7);
 * this screen stays focused on the contractor's perspective.
 */
@Composable
fun OrderDetailScreen(
    orderId: String,
    onSubmitBid: (Order) -> Unit,
    paddingValues: PaddingValues = PaddingValues(),
    viewModel: MarketplaceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val order: Order? = when (val s = state) {
        is RemoteState.Loaded -> s.value.firstOrNull { it.id == orderId }
        else -> null
    }

    if (order == null) {
        LoadingStateView(
            title = stringResource(com.spectech.uikit.R.string.state_loading),
            paddingValues = paddingValues,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeaderCard(order)
        DetailsCard(order)
        BidsSummaryCard(order)

        if (viewModel.isAuthenticated && order.status == OrderStatus.OPEN && !order.isExpired) {
            Button(
                onClick = { onSubmitBid(order) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    text = stringResource(R.string.detail_submit_bid),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HeaderCard(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = order.equipmentCategory?.label()
                        ?: stringResource(R.string.detail_no_category),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                OrderStatusBadge(
                    status = order.status,
                    label = order.status.label(),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp),
                )
                Text(
                    text = order.fullAddress,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            order.createdAt?.let {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                    Text(
                        text = it.relativeFromNow(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsCard(order: Order) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.detail_section_terms),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            order.pricingUnit?.let { unit ->
                DetailRow(
                    label = stringResource(R.string.detail_pricing_unit),
                    value = unit.label(),
                )
            }
            if (order.paymentTypes.isNotEmpty()) {
                DetailRow(
                    label = stringResource(R.string.detail_payment),
                    value = order.paymentTypes.joinToString(" • ") { it.label(context) },
                )
            }
            order.workVolume?.let { volume ->
                DetailRow(
                    label = stringResource(R.string.detail_work_volume),
                    value = if (volume % 1.0 == 0.0) volume.toInt().toString() else volume.toString(),
                )
            }
            order.durationHours?.let { hrs ->
                DetailRow(
                    label = stringResource(R.string.detail_duration_hours),
                    value = hrs.toString(),
                )
            }
            order.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Text(
                    text = stringResource(R.string.detail_description),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(text = desc, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun BidsSummaryCard(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.detail_section_bids, order.bidCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.detail_bids_summary_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
