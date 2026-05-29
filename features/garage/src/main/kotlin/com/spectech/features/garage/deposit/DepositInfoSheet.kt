package com.spectech.features.garage.deposit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.spectech.domain.enums.DepositStatus
import com.spectech.domain.model.Deposit
import com.spectech.features.garage.R
import com.spectech.platform.web.CustomTabsLauncher
import com.spectech.uikit.strings.localizedMessage
import com.spectech.uikit.theme.SuccessGreen
import com.spectech.uikit.theme.WarningAmber
import java.text.NumberFormat
import java.util.Locale

/**
 * Modal sheet showing the deposit status for one equipment unit. Mirrors
 * iOS `DepositInfoSheet`. The Pay button launches Chrome Custom Tabs;
 * returning to the app triggers a status sync via a lifecycle observer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositInfoSheet(
    equipmentId: String,
    onDismiss: () -> Unit,
    viewModel: DepositInfoViewModel = hiltViewModel(
        creationCallback = { factory: DepositInfoViewModel.Factory -> factory.create(equipmentId) },
    ),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(equipmentId) {
        viewModel.load()
    }

    // Launch the pending Custom Tab URL when the VM publishes one.
    LaunchedEffect(viewModel.pendingConfirmationUrl) {
        val url = viewModel.pendingConfirmationUrl ?: return@LaunchedEffect
        CustomTabsLauncher.launch(context, url)
        viewModel.consumePendingConfirmationUrl()
    }

    // Sync deposit status when the user returns from the Custom Tab.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncOnReturn()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        DepositSheetContent(viewModel = viewModel, onClose = onDismiss)
    }
}

@Composable
private fun DepositSheetContent(
    viewModel: DepositInfoViewModel,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.deposit_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.deposit_close))
            }
        }

        when {
            viewModel.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
            else -> DepositStateView(viewModel = viewModel)
        }

        viewModel.error?.let { err ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = err.localizedMessage(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DepositStateView(viewModel: DepositInfoViewModel) {
    val deposit = viewModel.existingDeposit

    when (deposit?.status) {
        null, DepositStatus.FAILED, DepositStatus.REFUNDED, DepositStatus.FORFEITED ->
            NoActiveDepositCard(viewModel, terminalDeposit = deposit)

        DepositStatus.PENDING -> PendingDepositCard(viewModel, deposit)

        DepositStatus.PAID -> PaidDepositCard(viewModel, deposit)

        DepositStatus.REFUND_PENDING -> InfoBanner(
            icon = Icons.Outlined.HourglassBottom,
            tint = WarningAmber,
            headline = stringResource(R.string.deposit_refund_pending_headline),
            message = stringResource(R.string.deposit_refund_pending_message),
            amount = deposit.amount.toPlainString(),
            currency = deposit.currency,
        )
    }
}

@Composable
private fun NoActiveDepositCard(
    viewModel: DepositInfoViewModel,
    terminalDeposit: Deposit?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.deposit_intro_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.deposit_intro_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            terminalDeposit?.let { d ->
                Text(
                    text = stringResource(
                        when (d.status) {
                            DepositStatus.FAILED -> R.string.deposit_history_failed
                            DepositStatus.REFUNDED -> R.string.deposit_history_refunded
                            DepositStatus.FORFEITED -> R.string.deposit_history_forfeited
                            else -> R.string.deposit_history_unknown
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PrimaryButton(
                label = stringResource(R.string.deposit_action_pay),
                inFlight = viewModel.isPayInFlight,
                onClick = viewModel::startPayment,
            )
        }
    }
}

@Composable
private fun PendingDepositCard(viewModel: DepositInfoViewModel, deposit: Deposit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.HourglassBottom,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.deposit_pending_headline),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            AmountRow(deposit.amount.toPlainString(), deposit.currency)
            Text(
                text = stringResource(R.string.deposit_pending_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (viewModel.isSyncInFlight) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.deposit_syncing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            PrimaryButton(
                label = stringResource(R.string.deposit_action_continue),
                inFlight = viewModel.isPayInFlight,
                onClick = viewModel::startPayment,
            )
        }
    }
}

@Composable
private fun PaidDepositCard(viewModel: DepositInfoViewModel, deposit: Deposit) {
    var showRefundConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SuccessGreen.copy(alpha = 0.12f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.deposit_paid_headline),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            AmountRow(deposit.amount.toPlainString(), deposit.currency)
            Text(
                text = stringResource(R.string.deposit_paid_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { showRefundConfirm = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(50),
            ) {
                if (viewModel.isRefundInFlight) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text(stringResource(R.string.deposit_action_refund))
                }
            }
        }
    }

    if (showRefundConfirm) {
        AlertDialog(
            onDismissRequest = { showRefundConfirm = false },
            title = { Text(stringResource(R.string.deposit_refund_confirm_title)) },
            text = { Text(stringResource(R.string.deposit_refund_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showRefundConfirm = false
                    viewModel.refund()
                }) {
                    Text(
                        text = stringResource(R.string.deposit_refund_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRefundConfirm = false }) {
                    Text(stringResource(R.string.deposit_refund_cancel))
                }
            },
        )
    }
}

@Composable
private fun InfoBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    headline: String,
    message: String,
    amount: String,
    currency: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(text = headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            AmountRow(amount, currency)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AmountRow(amount: String, currency: String) {
    val parsed = amount.toBigDecimalOrNull()
    val display = parsed?.let {
        // Russian-style amount with currency suffix; iOS does the same.
        NumberFormat.getNumberInstance(Locale.getDefault()).format(it)
    } ?: amount
    Text(
        text = "$display $currency",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun PrimaryButton(label: String, inFlight: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !inFlight,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        if (inFlight) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(text = label, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}
