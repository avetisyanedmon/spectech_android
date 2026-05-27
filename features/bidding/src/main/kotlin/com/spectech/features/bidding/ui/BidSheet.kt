package com.spectech.features.bidding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spectech.domain.enums.PaymentType
import com.spectech.domain.model.Equipment
import com.spectech.domain.model.Order
import com.spectech.features.bidding.R
import com.spectech.features.bidding.viewmodel.BidSheetViewModel
import com.spectech.uikit.strings.label

/**
 * Bid Sheet shown over an order detail. Hosted at `:app` level so it can
 * coordinate the "Go to Garage to add equipment" navigation when the
 * contractor has nothing matching the order's category.
 *
 * iOS analog: SwiftUI `BidSheetView` presented via `.sheet` from
 * `OrderDetailView`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BidSheet(
    order: Order,
    onDismiss: () -> Unit,
    onRequestAddEquipment: () -> Unit,
    viewModel: BidSheetViewModel = hiltViewModel(
        creationCallback = { factory: BidSheetViewModel.Factory -> factory.create(order) },
    ),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(order.id) {
        viewModel.load()
    }
    LaunchedEffect(viewModel.success) {
        if (viewModel.success) {
            viewModel.reset()
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        BidForm(
            viewModel = viewModel,
            onClose = onDismiss,
            onRequestAddEquipment = onRequestAddEquipment,
        )
    }
}

@Composable
private fun BidForm(
    viewModel: BidSheetViewModel,
    onClose: () -> Unit,
    onRequestAddEquipment: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {

        // Header --------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.bid_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.bid_close))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            if (viewModel.availableEquipment.isEmpty()) {
                // No matching equipment — direct the contractor to the garage.
                NoEquipmentCard(
                    onAddEquipment = {
                        onClose()
                        onRequestAddEquipment()
                    },
                )
            } else {
                EquipmentPicker(
                    items = viewModel.availableEquipment,
                    selected = viewModel.selectedEquipment,
                    onSelect = { viewModel.selectedEquipment = it },
                )
            }

            // Price + delivery ----------------------------------------------
            OutlinedTextField(
                value = viewModel.price,
                onValueChange = {
                    viewModel.price = it.filter { ch -> ch.isDigit() || ch == '.' }
                },
                label = { Text(stringResource(R.string.bid_price)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.deliveryPrice,
                onValueChange = {
                    viewModel.deliveryPrice = it.filter { ch -> ch.isDigit() || ch == '.' }
                },
                label = { Text(stringResource(R.string.bid_delivery_price)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            // Payment type chips (constrained to order's accepted set) ------
            Text(
                text = stringResource(R.string.bid_payment),
                style = MaterialTheme.typography.titleSmall,
            )
            PaymentChips(
                accepted = viewModel.order.paymentTypes,
                selected = viewModel.paymentType,
                onSelect = { viewModel.paymentType = it },
            )

            // Comment -------------------------------------------------------
            OutlinedTextField(
                value = viewModel.comment,
                onValueChange = { viewModel.comment = it },
                label = { Text(stringResource(R.string.bid_comment)) },
                modifier = Modifier.fillMaxWidth().height(96.dp),
            )

            viewModel.error?.let { err -> BidErrorBanner(err) }

            Spacer(Modifier.height(8.dp))
        }

        // Submit -------------------------------------------------------------
        Button(
            onClick = { viewModel.submit() },
            enabled = viewModel.canSubmit && viewModel.availableEquipment.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            if (viewModel.isSubmitting) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Text(
                    text = stringResource(R.string.bid_submit),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun BidErrorBanner(error: com.spectech.domain.error.ApiError) {
    val message = when {
        error.code == BidSheetViewModel.DEPOSIT_REQUIRED_CODE ||
            error.statusCode == BidSheetViewModel.DEPOSIT_REQUIRED_STATUS ->
            stringResource(R.string.bid_error_deposit_required)
        error.code == BidSheetViewModel.OWN_ORDER_CODE ->
            stringResource(R.string.bid_error_own_order)
        error.code == BidSheetViewModel.INVALID_PRICE_CODE ->
            stringResource(R.string.bid_error_invalid_price)
        error.code == BidSheetViewModel.INVALID_PAYMENT_CODE ->
            stringResource(R.string.bid_error_invalid_payment)
        else -> error.message
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EquipmentPicker(
    items: List<Equipment>,
    selected: Equipment?,
    onSelect: (Equipment) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val display = selected?.name ?: stringResource(R.string.bid_select_equipment)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.bid_equipment_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(item.name)
                            item.category?.let { cat ->
                                Text(
                                    text = cat.label(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PaymentChips(
    accepted: List<PaymentType>,
    selected: PaymentType?,
    onSelect: (PaymentType) -> Unit,
) {
    if (accepted.isEmpty()) {
        Text(
            text = stringResource(R.string.bid_no_accepted_payments),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        accepted.forEach { type ->
            FilterChip(
                selected = type == selected,
                onClick = { onSelect(type) },
                label = { Text(type.label()) },
            )
        }
    }
}

@Composable
private fun NoEquipmentCard(onAddEquipment: () -> Unit) {
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
                text = stringResource(R.string.bid_no_equipment_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.bid_no_equipment_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onAddEquipment,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(50),
            ) {
                Text(stringResource(R.string.bid_no_equipment_cta))
            }
        }
    }
}
