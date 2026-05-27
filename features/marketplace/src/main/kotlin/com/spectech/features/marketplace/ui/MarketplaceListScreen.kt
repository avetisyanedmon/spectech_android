package com.spectech.features.marketplace.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectech.domain.state.RemoteState
import com.spectech.features.marketplace.R
import com.spectech.features.marketplace.ui.components.OrderCardView
import com.spectech.features.marketplace.viewmodel.MarketplaceViewModel
import com.spectech.uikit.components.EmptyStateView
import com.spectech.uikit.components.ErrorStateView
import com.spectech.uikit.components.LoadingStateView

@Composable
fun MarketplaceListScreen(
    onOrderClick: (String) -> Unit,
    onOpenFilters: () -> Unit,
    paddingValues: PaddingValues = PaddingValues(),
    viewModel: MarketplaceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val visible by viewModel.visibleOrders.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (state is RemoteState.Idle) viewModel.load()
    }

    when (val current = state) {
        RemoteState.Idle, RemoteState.Loading -> LoadingStateView(
            title = stringResource(com.spectech.uikit.R.string.state_loading),
            paddingValues = paddingValues,
        )

        is RemoteState.Empty -> EmptyStateView(
            title = stringResource(R.string.marketplace_empty_title),
            message = stringResource(R.string.marketplace_empty_message),
            actionTitle = stringResource(com.spectech.uikit.R.string.state_retry),
            icon = Icons.Outlined.Inventory2,
            onAction = { viewModel.load(forceRefresh = true) },
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
            if (visible.isEmpty()) {
                EmptyStateView(
                    title = stringResource(R.string.marketplace_empty_title),
                    message = stringResource(R.string.marketplace_empty_filtered),
                    actionTitle = stringResource(R.string.marketplace_open_filters),
                    icon = Icons.Outlined.Inventory2,
                    onAction = onOpenFilters,
                    paddingValues = paddingValues,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = visible, key = { it.id }) { order ->
                        OrderCardView(
                            order = order,
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
}
