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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.spectech.domain.model.Bid
import com.spectech.domain.model.Order
import com.spectech.domain.state.RemoteState
import com.spectech.features.orders.R
import com.spectech.features.orders.ui.components.RevealedContactCard
import com.spectech.features.orders.viewmodel.MyOrdersViewModel
import com.spectech.uikit.components.LoadingStateView
import com.spectech.uikit.components.OrderStatusBadge
import com.spectech.uikit.strings.label

/**
 * Customer's view of their own order. Shows incoming bids; each bid has an
 * Accept button (only when the order is still open and no other bid has
 * been accepted yet). On acceptance, the chosen contractor's contact is
 * revealed inline.
 */
@Composable
fun MyOrderDetailScreen(
    orderId: String,
    paddingValues: PaddingValues = PaddingValues(),
    viewModel: MyOrdersViewModel,
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

    val revealed = viewModel.revealedContacts

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OrderHeaderCard(order)
        BidsListCard(
            order = order,
            revealedContacts = revealed,
            onAccept = { bid -> viewModel.acceptBid(order.id, bid.id) },
            acceptError = viewModel.acceptError,
        )
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
private fun BidsListCard(
    order: Order,
    revealedContacts: Map<String, com.spectech.domain.model.ContractorContact>,
    onAccept: (Bid) -> Unit,
    acceptError: com.spectech.domain.error.ApiError?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.detail_section_bids, order.bidCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (order.bids.isEmpty()) {
                Text(
                    text = stringResource(R.string.detail_bids_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                order.bids.forEach { bid ->
                    BidRow(
                        bid = bid,
                        canAccept = order.acceptedBidId == null && !bid.isAccepted,
                        contactReveal = bid.contractorContact?.takeIf { bid.isAccepted }
                            ?: revealedContacts[bid.id]
                                ?.takeIf { bid.isAccepted || revealedContacts.containsKey(bid.id) },
                        onAccept = { onAccept(bid) },
                    )
                }
            }
            acceptError?.let { err ->
                Text(
                    text = err.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun BidRow(
    bid: Bid,
    canAccept: Boolean,
    contactReveal: com.spectech.domain.model.ContractorContact?,
    onAccept: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = bid.equipmentName ?: bid.contractorName.orEmpty().ifBlank { "—" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = bid.price.toPlainString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (canAccept) {
                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.height(40.dp),
                ) {
                    Text(stringResource(R.string.accept_bid))
                }
            } else if (bid.isAccepted) {
                Text(
                    text = stringResource(R.string.bid_status_accepted),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        contactReveal?.phone?.let { phone ->
            RevealedContactCard(
                headline = stringResource(R.string.reveal_contractor_contact),
                phone = phone,
                contactName = contactReveal.name ?: bid.contractorName,
            )
        }
    }
}
