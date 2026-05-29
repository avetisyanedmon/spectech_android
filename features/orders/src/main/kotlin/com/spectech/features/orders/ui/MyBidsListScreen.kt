package com.spectech.features.orders.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectech.domain.model.Order
import com.spectech.domain.state.RemoteState
import com.spectech.features.orders.R
import com.spectech.features.orders.ui.components.MyBidCard
import com.spectech.features.orders.ui.components.MyBidStatus
import com.spectech.features.orders.ui.components.WithdrawBidButton
import com.spectech.features.orders.viewmodel.MyBidsViewModel
import com.spectech.uikit.components.EmptyStateView
import com.spectech.uikit.components.ErrorStateView
import com.spectech.uikit.components.LoadingStateView

/**
 * Contractor's "My Bids" tab. Mirrors iOS `MyBidsView`
 * (SpecTechIOS/Scene/Tabs/MyBids/MyBidsView.swift):
 *
 *   - Tap card → order detail
 *   - Inline [WithdrawBidButton] directly under each non-accepted bid
 *     (was previously only on the detail screen)
 *   - Rich [MyBidCard] shows category, status pill, order ID + copy, address,
 *     "My price" + "Delivery", start datetime, and a customer-contact block
 *     once the bid is accepted
 */
@Composable
fun MyBidsListScreen(
    onOrderClick: (String) -> Unit,
    paddingValues: PaddingValues = PaddingValues(),
    viewModel: MyBidsViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (state is RemoteState.Idle) viewModel.load()
    }

    when (val current = state) {
        RemoteState.Idle, RemoteState.Loading -> LoadingStateView(
            title = stringResource(com.spectech.uikit.R.string.state_loading),
            paddingValues = paddingValues,
        )

        is RemoteState.Empty -> EmptyStateView(
            title = stringResource(R.string.my_bids_empty_title),
            message = stringResource(R.string.my_bids_empty_message),
            icon = Icons.Outlined.AssignmentTurnedIn,
            paddingValues = paddingValues,
        )

        is RemoteState.Failed -> ErrorStateView(
            error = current.error,
            title = stringResource(com.spectech.uikit.R.string.state_error_title),
            retryTitle = stringResource(com.spectech.uikit.R.string.state_retry),
            onRetry = { viewModel.load(forceRefresh = true) },
            paddingValues = paddingValues,
        )

        is RemoteState.Loaded -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = current.value, key = { it.id }) { order ->
                    MyBidRow(
                        order = order,
                        viewModel = viewModel,
                        onClick = { onOrderClick(order.id) },
                    )
                    LaunchedEffect(order.id) {
                        viewModel.loadMoreIfNeeded(order)
                    }
                }
            }
        }
    }
}

/**
 * One list row — the [MyBidCard] plus an inline [WithdrawBidButton] when the
 * bid is still pending. Kept as a separate composable so the LazyColumn key
 * scopes the withdraw state per-order.
 */
@Composable
private fun MyBidRow(
    order: Order,
    viewModel: MyBidsViewModel,
    onClick: () -> Unit,
) {
    val myBid = viewModel.myBid(order)
    val status = when {
        myBid == null -> MyBidStatus.Pending // no bid means the row shouldn't even be here
        myBid.isAccepted -> MyBidStatus.Accepted
        order.acceptedBidId != null -> MyBidStatus.NotSelected
        else -> MyBidStatus.Pending
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MyBidCard(
            order = order,
            myBid = myBid,
            status = status,
            onClick = onClick,
        )
        // iOS shows the withdraw button under the card whenever the user's
        // bid exists and hasn't been accepted yet. Match that placement.
        if (myBid != null && !myBid.isAccepted && order.acceptedBidId == null) {
            WithdrawBidButton(
                onConfirm = { viewModel.withdrawBid(order.id, myBid.id) },
            )
        }
    }
}
