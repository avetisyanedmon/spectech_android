package com.spectech.features.orders.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPasteSearch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectech.domain.state.RemoteState
import com.spectech.features.orders.R
import com.spectech.features.orders.ui.components.OrderListItemCard
import com.spectech.features.orders.viewmodel.MyOrdersViewModel
import com.spectech.uikit.components.EmptyStateView
import com.spectech.uikit.components.ErrorStateView
import com.spectech.uikit.components.LoadingStateView

@Composable
fun MyOrdersListScreen(
    onOrderClick: (String) -> Unit,
    paddingValues: PaddingValues = PaddingValues(),
    viewModel: MyOrdersViewModel,
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
            title = stringResource(R.string.my_orders_empty_title),
            message = stringResource(R.string.my_orders_empty_message),
            icon = Icons.Outlined.ContentPasteSearch,
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
                    OrderListItemCard(
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
