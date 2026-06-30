package com.spectech.features.createOrder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spectech.features.createOrder.R
import com.spectech.features.createOrder.ui.components.CategoryOptionsSection
import com.spectech.features.createOrder.ui.components.CategoryPicker
import com.spectech.features.createOrder.ui.components.CitySearchField
import com.spectech.features.createOrder.ui.components.DateField
import com.spectech.features.createOrder.ui.components.DateTimeField
import com.spectech.features.createOrder.ui.components.PaymentTypeChips
import com.spectech.features.createOrder.ui.components.PricingUnitPicker
import com.spectech.features.createOrder.ui.components.RegionPickerField
import com.spectech.features.createOrder.ui.components.TimeField
import com.spectech.features.createOrder.viewmodel.CreateOrderViewModel
import com.spectech.uikit.strings.localizedMessage

/**
 * Modal sheet that hosts the Create Order form. Presented from the
 * Marketplace tab's "+" button. Auto-dismisses on submit success and refreshes
 * the marketplace list via [com.spectech.data.events.DomainEvent.OrdersChanged].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderSheet(
    onDismiss: () -> Unit,
    viewModel: CreateOrderViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(viewModel.success) {
        if (viewModel.success) {
            viewModel.reset()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        CreateOrderForm(viewModel = viewModel, onClose = onDismiss)
    }
}

@Composable
private fun CreateOrderForm(viewModel: CreateOrderViewModel, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        // Header --------------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.create_order_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.create_order_close),
                )
            }
        }

        // Scrollable form -----------------------------------------------------
        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            Section(stringResource(R.string.create_order_section_equipment)) {
                CategoryPicker(
                    value = viewModel.category,
                    onValueChange = { viewModel.category = it },
                    label = stringResource(R.string.create_order_category),
                )
                // Per-category optional parameters (boom length, attachments,
                // axle config, etc.) Mirrors iOS `CategoryOptionsSection` —
                // hidden for categories that don't have any extra fields.
                CategoryOptionsSection(
                    category = viewModel.category,
                    options = viewModel.categoryOptions,
                )
            }

            Section(stringResource(R.string.create_order_section_address)) {
                RegionPickerField(
                    value = viewModel.region,
                    onValueChange = { newRegion ->
                        // Match iOS line 428-430: clear the city when the
                        // region changes so an old city under the previous
                        // region can't leak into the new one.
                        if (newRegion != viewModel.region) viewModel.city = ""
                        viewModel.region = newRegion
                    },
                    label = stringResource(R.string.create_order_region),
                )
                CitySearchField(
                    value = viewModel.city,
                    onValueChange = { viewModel.city = it },
                    label = stringResource(R.string.create_order_city),
                    region = viewModel.region,
                )
                OutlinedTextField(
                    value = viewModel.street,
                    onValueChange = { viewModel.street = it },
                    label = { Text(stringResource(R.string.create_order_street_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = viewModel.houseNumber,
                    onValueChange = { viewModel.houseNumber = it },
                    label = { Text(stringResource(R.string.create_order_house_number_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Section(stringResource(R.string.create_order_section_pricing)) {
                PricingUnitPicker(
                    value = viewModel.pricingUnit,
                    onValueChange = { viewModel.pricingUnit = it },
                    label = stringResource(R.string.create_order_pricing_unit),
                )
                OutlinedTextField(
                    value = viewModel.workVolume,
                    onValueChange = { viewModel.updateWorkVolume(it) },
                    label = { Text(stringResource(R.string.create_order_work_volume)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.create_order_payment),
                    style = MaterialTheme.typography.titleSmall,
                )
                PaymentTypeChips(
                    selected = viewModel.selectedPaymentType,
                    onSelect = { viewModel.selectedPaymentType = it },
                )
            }

            Section(stringResource(R.string.create_order_section_schedule)) {
                DateField(
                    label = stringResource(R.string.create_order_start_date),
                    value = viewModel.startDate,
                    onValueChange = { viewModel.updateStartDate(it) },
                )
                TimeField(
                    label = stringResource(R.string.create_order_start_time),
                    value = viewModel.startTime,
                    onValueChange = { viewModel.updateStartTime(it) },
                    error = viewModel.startTimeValidationError,
                )
                DateTimeField(
                    label = stringResource(R.string.create_order_bidding_deadline),
                    value = viewModel.biddingDeadline,
                    onValueChange = { viewModel.updateBiddingDeadline(it) },
                )
            }

            Section(stringResource(R.string.create_order_section_description)) {
                OutlinedTextField(
                    value = viewModel.description,
                    onValueChange = { viewModel.description = it },
                    label = { Text(stringResource(R.string.create_order_description)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                )
            }

            viewModel.error?.let { err ->
                Text(
                    text = err.localizedMessage(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        // Bottom button -------------------------------------------------------
        Button(
            onClick = { viewModel.submit() },
            enabled = viewModel.isFormValid && !viewModel.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            if (viewModel.isSubmitting) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Text(
                    text = stringResource(R.string.create_order_submit),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

