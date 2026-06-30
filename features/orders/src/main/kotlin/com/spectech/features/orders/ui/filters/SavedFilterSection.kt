package com.spectech.features.orders.ui.filters

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectech.domain.model.OrderFilters
import com.spectech.features.marketplace.savedfilter.SavedFilterViewModel
import com.spectech.features.orders.R

/**
 * "Save these filters + notify me" section for My Orders filter sheet.
 * Reuses SavedFilterViewModel from the marketplace, which manages a
 * global saved filter and notifications state across the app.
 *
 * The save button writes [draft] verbatim — the user's intent is to subscribe
 * to the picks currently in the sheet, even if they haven't tapped Apply yet.
 * The toggle requires a non-empty saved filter; flipping it on without one
 * surfaces the same error iOS shows.
 *
 * Runtime POST_NOTIFICATIONS permission is requested inline on API 33+.
 */
@Composable
fun SavedFilterSection(
    draft: OrderFilters,
    modifier: Modifier = Modifier,
    viewModel: SavedFilterViewModel = hiltViewModel(),
) {
    val saved by viewModel.savedFilter.collectAsStateWithLifecycle()
    val enabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val lastError by viewModel.lastSyncError.collectAsStateWithLifecycle()

    val ctx = LocalContext.current
    var pendingEnableAfterPermission by remember { mutableStateOf(false) }
    var inlineError by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && pendingEnableAfterPermission) {
            viewModel.setNotificationsEnabled(true)
        } else if (!granted) {
            inlineError = ctx.getString(R.string.filters_saved_permission_required)
        }
        pendingEnableAfterPermission = false
    }

    LaunchedEffect(enabled, saved) {
        if (inlineError != null) inlineError = null
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.filters_saved_section),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.filters_saved_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = { viewModel.save(draft) },
            enabled = !draft.isEmpty && !isSyncing,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(stringResource(R.string.filters_save_button))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.filters_saved_toggle),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = enabled,
                onCheckedChange = { wantEnabled ->
                    when {
                        !wantEnabled -> viewModel.setNotificationsEnabled(false)
                        saved == null ->
                            inlineError = ctx.getString(R.string.filters_saved_empty_error)
                        else -> requestPermissionThenEnable(
                            ctx = ctx,
                            launcher = permissionLauncher,
                            setPending = { pendingEnableAfterPermission = it },
                            onAlreadyGranted = { viewModel.setNotificationsEnabled(true) },
                        )
                    }
                },
                enabled = !isSyncing,
            )
        }

        if (isSyncing) {
            Text(
                text = stringResource(R.string.filters_saved_syncing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val errorToShow = lastError ?: inlineError
        if (errorToShow != null) {
            Text(
                text = errorToShow,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (saved != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { viewModel.clear() }, enabled = !isSyncing) {
                    Text(
                        text = stringResource(R.string.filters_clear_saved),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

private fun requestPermissionThenEnable(
    ctx: android.content.Context,
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
    setPending: (Boolean) -> Unit,
    onAlreadyGranted: () -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        onAlreadyGranted()
        return
    }
    val granted = ContextCompat.checkSelfPermission(
        ctx,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
    if (granted) {
        onAlreadyGranted()
        return
    }
    setPending(true)
    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
}
