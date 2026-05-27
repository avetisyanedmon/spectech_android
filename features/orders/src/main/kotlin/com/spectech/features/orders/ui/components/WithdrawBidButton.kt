package com.spectech.features.orders.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spectech.features.orders.R

/**
 * Destructive button with a confirmation dialog. Mirrors iOS
 * `WithdrawBidButton` (SpecTechIOS/Scene/Tabs/MyBids/Components/WithdrawBidButton.swift).
 */
@Composable
fun WithdrawBidButton(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showingConfirm by remember { mutableStateOf(false) }

    Button(
        onClick = { showingConfirm = true },
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
        ),
    ) {
        Text(
            text = stringResource(R.string.withdraw_bid),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
    }

    if (showingConfirm) {
        AlertDialog(
            onDismissRequest = { showingConfirm = false },
            title = { Text(stringResource(R.string.withdraw_bid_confirm_title)) },
            text = { Text(stringResource(R.string.withdraw_bid_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showingConfirm = false
                    onConfirm()
                }) {
                    Text(
                        text = stringResource(R.string.withdraw_bid_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showingConfirm = false }) {
                    Text(stringResource(R.string.withdraw_bid_cancel))
                }
            },
        )
    }
}
