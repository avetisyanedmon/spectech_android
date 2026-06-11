package com.spectech.features.orders.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPasteSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectech.domain.state.RemoteState
import com.spectech.features.marketplace.ui.components.OrderCardView
import com.spectech.features.orders.R
import com.spectech.features.orders.viewmodel.MyOrdersViewModel
import com.spectech.uikit.components.EmptyStateView
import com.spectech.uikit.components.ErrorStateView
import com.spectech.uikit.components.LoadingStateView
import kotlinx.coroutines.launch

/**
 * Customer's "My Orders" list. Mirrors iOS `MyOrdersView`
 * (SpecTechIOS/Scene/Tabs/MyOrders/MyOrdersView.swift):
 *
 *   - Reuses the full marketplace [OrderCardView] for visual parity, with
 *     `showOrderId = true`, `isOwnOrder = true`, and `currentUserId` wired
 *     up so the "Offer accepted" / contact-reveal branches activate the
 *     same way they do on iOS.
 *   - Pull-to-refresh triggers a full reload of `?view=mine`.
 *   - Loading / empty / failed states defer to the shared state-view
 *     components.
 *
 * Tapping a card forwards the order id to [onOrderClick]; navigation to the
 * detail screen lives in [com.spectech.features.orders.navigation.MyOrdersNavGraph].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersListScreen(
    onOrderClick: (String) -> Unit,
    paddingValues: PaddingValues = PaddingValues(),
    viewModel: MyOrdersViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (state is RemoteState.Idle) viewModel.load()
    }

    val pullState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
            // Reset the spinner once the VM has settled back into Loaded.
            LaunchedEffect(current.value) { isRefreshing = false }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                state = pullState,
                onRefresh = {
                    isRefreshing = true
                    scope.launch { viewModel.load(forceRefresh = true) }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items = current.value, key = { it.id }) { order ->
                            val isOwn = remember(order, currentUserId) {
                                isCreatedByCurrentUser(order.creatorId, currentUserId)
                            }
                            OrderCardView(
                                order = order,
                                onClick = { onOrderClick(order.id) },
                                currentUserId = currentUserId,
                                isOwnOrder = isOwn,
                                showOrderId = true,
                                showBidCount = true,
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
}

/**
 * iOS computes `isOwn` per-row as
 * `currentUserId.uuidString.lowercased() == order.creatorId.lowercased()`.
 * We mirror that to keep the "Offer accepted" / contractor-contact branches
 * lighting up under the same conditions on both platforms — and to guard
 * against backend responses that ever return an order on `?view=mine`
 * whose creator doesn't match (e.g. a shared/legacy id).
 */
private fun isCreatedByCurrentUser(creatorId: String?, currentUserId: String?): Boolean {
    val uid = currentUserId?.lowercase() ?: return false
    val cid = creatorId?.lowercase() ?: return false
    return uid == cid
}
