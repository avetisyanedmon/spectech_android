package com.spectech.features.orders.ui

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectech.domain.model.Order
import com.spectech.domain.state.RemoteState
import com.spectech.features.orders.R
import com.spectech.features.orders.ui.components.RevealedContactCard
import com.spectech.features.orders.ui.components.WithdrawBidButton
import com.spectech.features.orders.viewmodel.MyBidsViewModel
import com.spectech.uikit.components.LoadingStateView
import com.spectech.uikit.components.OrderStatusBadge
import com.spectech.uikit.strings.label
import com.spectech.uikit.strings.localizedMessage

/**
 * Contractor's view of an order they've bid on. Shows the order header, the
 * contractor's own bid, and a withdraw button. When the customer has accepted
 * this bid, the order's `creatorPhone` field is populated by the backend and
 * we surface it as a revealed contact card.
 */
@Composable
fun MyBidDetailScreen(
    orderId: String,
    paddingValues: PaddingValues = PaddingValues(),
    viewModel: MyBidsViewModel,
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

    val myBid = viewModel.myBid(order)
    val isAccepted = myBid?.isAccepted == true || order.acceptedBidId == myBid?.id

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OrderHeaderCard(order)

        if (isAccepted && !order.creatorPhone.isNullOrBlank()) {
            RevealedContactCard(
                headline = stringResource(R.string.reveal_customer_contact),
                phone = order.creatorPhone!!,
                contactName = order.creatorName,
            )
        }

        myBid?.let { bid ->
            MyBidCard(bid = bid)
            viewModel.withdrawError?.let { err ->
                Text(
                    text = err.localizedMessage(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (!bid.isAccepted && order.acceptedBidId == null) {
                WithdrawBidButton(onConfirm = {
                    viewModel.withdrawBid(order.id, bid.id)
                })
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OrderHeaderCard(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = order.equipmentCategory?.label() ?: "—",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                OrderStatusBadge(status = order.status, label = order.status.label())
            }
            Text(
                text = order.fullAddress,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MyBidCard(bid: com.spectech.domain.model.Bid) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.detail_section_my_bid),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.detail_bid_price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = bid.price.toPlainString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            bid.equipmentName?.let { name ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.detail_bid_equipment),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            bid.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                Text(
                    text = comment,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
