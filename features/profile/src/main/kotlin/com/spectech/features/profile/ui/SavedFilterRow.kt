package com.spectech.features.profile.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectech.features.profile.R
import com.spectech.features.profile.viewmodel.ProfileViewModel

/**
 * Saved-filter notifications toggle exposed in the Profile menu. Mirrors the
 * "Notify me about matching orders" switch from the marketplace filter sheet
 * — same store, so flipping it here updates both surfaces.
 *
 * The switch is disabled when no filter is saved (the backend requires a
 * non-empty filter); the helper text tells the user where to set one.
 */
@Composable
fun SavedFilterRow(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel,
) {
    val savedFilter by viewModel.savedFilterFlow.collectAsStateWithLifecycle()
    val enabled by viewModel.savedFilterNotificationsEnabledFlow.collectAsStateWithLifecycle()
    val isSyncing by viewModel.savedFilterSyncingFlow.collectAsStateWithLifecycle()

    val ctx = LocalContext.current
    var pendingEnableAfterPermission by remember { mutableStateOf(false) }
    var permissionDeniedMsg by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && pendingEnableAfterPermission) {
            viewModel.setSavedFilterNotificationsEnabled(true)
        } else if (!granted) {
            permissionDeniedMsg = ctx.getString(R.string.profile_saved_filter_permission_required)
        }
        pendingEnableAfterPermission = false
    }

    LaunchedEffect(enabled, savedFilter) {
        if (permissionDeniedMsg != null) permissionDeniedMsg = null
    }

    val hasSavedFilter = savedFilter != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_saved_filter_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.profile_saved_filter_toggle),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = { wantEnabled ->
                        if (wantEnabled) {
                            requestPermissionThenEnable(
                                ctx = ctx,
                                launcher = permissionLauncher,
                                setPending = { pendingEnableAfterPermission = it },
                                onAlreadyGranted = {
                                    viewModel.setSavedFilterNotificationsEnabled(true)
                                },
                            )
                        } else {
                            viewModel.setSavedFilterNotificationsEnabled(false)
                        }
                    },
                    enabled = hasSavedFilter && !isSyncing,
                )
            }
            if (!hasSavedFilter) {
                Text(
                    text = stringResource(R.string.profile_saved_filter_empty_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            permissionDeniedMsg?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun requestPermissionThenEnable(
    ctx: Context,
    launcher: ActivityResultLauncher<String>,
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
