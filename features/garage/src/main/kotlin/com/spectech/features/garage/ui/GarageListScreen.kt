package com.spectech.features.garage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Garage
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectech.domain.state.RemoteState
import com.spectech.features.garage.R
import com.spectech.features.garage.ui.components.EquipmentCard
import com.spectech.features.garage.viewmodel.GarageViewModel
import com.spectech.uikit.components.EmptyStateView
import com.spectech.uikit.components.ErrorStateView
import com.spectech.uikit.components.LoadingStateView

@Composable
fun GarageListScreen(
    onEquipmentClick: (String) -> Unit,
    paddingValues: PaddingValues = PaddingValues(),
    viewModel: GarageViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (state is RemoteState.Idle) viewModel.load()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(percent = 50),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(
                    text = stringResource(R.string.garage_add),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        },
    ) { innerPadding ->
        val combined = combinePadding(paddingValues, innerPadding)
        when (val current = state) {
            RemoteState.Idle, RemoteState.Loading -> LoadingStateView(
                title = stringResource(com.spectech.uikit.R.string.state_loading),
                paddingValues = combined,
            )

            is RemoteState.Empty -> EmptyStateView(
                title = stringResource(R.string.garage_empty_title),
                message = stringResource(R.string.garage_empty_message),
                icon = Icons.Outlined.Garage,
                paddingValues = combined,
            )

            is RemoteState.Failed -> ErrorStateView(
                error = current.error,
                title = stringResource(com.spectech.uikit.R.string.state_error_title),
                retryTitle = stringResource(com.spectech.uikit.R.string.state_retry),
                onRetry = { viewModel.load(forceRefresh = true) },
                paddingValues = combined,
            )

            is RemoteState.Loaded -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(combined),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = current.value, key = { it.id }) { equipment ->
                        EquipmentCard(
                            equipment = equipment,
                            onClick = { onEquipmentClick(equipment.id) },
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddEquipmentSheet(onDismiss = { showAddSheet = false })
    }
}

/** Sums the outer tab padding and the inner Scaffold padding. */
@Composable
private fun combinePadding(outer: PaddingValues, inner: PaddingValues): PaddingValues {
    val dir = LocalLayoutDirection.current
    return PaddingValues(
        start = outer.calculateStartPadding(dir) + inner.calculateStartPadding(dir),
        top = outer.calculateTopPadding() + inner.calculateTopPadding(),
        end = outer.calculateEndPadding(dir) + inner.calculateEndPadding(dir),
        bottom = outer.calculateBottomPadding() + inner.calculateBottomPadding(),
    )
}
