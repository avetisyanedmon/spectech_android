package com.spectech.features.news.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectech.domain.error.ApiError
import com.spectech.features.news.R
import com.spectech.features.news.viewmodel.NewsViewModel
import com.spectech.uikit.components.EmptyStateView
import com.spectech.uikit.components.ErrorStateView
import com.spectech.uikit.components.LoadingStateView

/**
 * Public news feed. Pull-to-refresh always available; empty/error/loaded
 * states mirror iOS `NewsView`. Items are keyed on `id` so the LazyColumn
 * preserves scroll position across refreshes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    paddingValues: PaddingValues = PaddingValues(),
    viewModel: NewsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val pullState = rememberPullToRefreshState()

    LaunchedEffect(Unit) { viewModel.loadInitial() }

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { viewModel.refresh() },
        state = pullState,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                items.isNotEmpty() -> NewsList(items, paddingValues)
                isLoading -> LoadingStateView(
                    title = stringResource(R.string.news_title),
                    paddingValues = paddingValues,
                )
                error != null -> ErrorStateView(
                    error = ApiError(message = error!!),
                    title = stringResource(R.string.news_error_title),
                    retryTitle = stringResource(R.string.news_retry),
                    onRetry = { viewModel.refresh() },
                    paddingValues = paddingValues,
                )
                else -> EmptyStateView(
                    title = stringResource(R.string.news_empty_title),
                    message = stringResource(R.string.news_empty_message),
                    icon = Icons.Outlined.Newspaper,
                    paddingValues = paddingValues,
                )
            }
        }
    }
}

@Composable
private fun NewsList(
    items: List<com.spectech.domain.model.NewsItem>,
    paddingValues: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = paddingValues.calculateTopPadding() + 12.dp,
            bottom = paddingValues.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
    ) {
        items(items = items, key = { it.id }) { item ->
            NewsCard(item = item)
        }
    }
}
