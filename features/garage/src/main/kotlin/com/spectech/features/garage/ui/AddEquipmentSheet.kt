package com.spectech.features.garage.ui

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
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
import com.spectech.domain.enums.EquipmentCategory
import com.spectech.features.garage.R
import com.spectech.features.garage.ui.components.PhotoGrid
import com.spectech.features.garage.viewmodel.AddEquipmentViewModel
import com.spectech.uikit.strings.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEquipmentSheet(
    onDismiss: () -> Unit,
    viewModel: AddEquipmentViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(viewModel.success) {
        if (viewModel.success) {
            viewModel.reset()
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        AddEquipmentForm(viewModel = viewModel, onClose = onDismiss)
    }
}

@Composable
private fun AddEquipmentForm(viewModel: AddEquipmentViewModel, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.add_equipment_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.add_equipment_close))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CategoryDropdown(
                selected = viewModel.category,
                onSelect = { viewModel.category = it },
            )

            OutlinedTextField(
                value = viewModel.name,
                onValueChange = { viewModel.name = it },
                label = { Text(stringResource(R.string.equipment_field_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.vin,
                onValueChange = { viewModel.vin = it },
                label = { Text(stringResource(R.string.equipment_field_vin)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.yearOfManufacture,
                onValueChange = { viewModel.yearOfManufacture = it.filter(Char::isDigit).take(4) },
                label = { Text(stringResource(R.string.equipment_field_year)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.description,
                onValueChange = { viewModel.description = it },
                label = { Text(stringResource(R.string.equipment_field_description)) },
                modifier = Modifier.fillMaxWidth().height(96.dp),
            )
            OutlinedTextField(
                value = viewModel.additionalEquipment,
                onValueChange = { viewModel.additionalEquipment = it },
                label = { Text(stringResource(R.string.equipment_field_additional)) },
                modifier = Modifier.fillMaxWidth().height(96.dp),
            )

            // Photos
            Text(
                text = stringResource(R.string.equipment_field_photos),
                style = MaterialTheme.typography.titleSmall,
            )
            PhotoGrid(
                pendingPhotoUris = viewModel.selectedPhotos.toList(),
                maxCount = AddEquipmentViewModel.MAX_PHOTOS,
                addLabel = stringResource(R.string.equipment_add_photo),
                onAdd = { uris -> viewModel.addPhotos(uris) },
                onRemovePending = { uri -> viewModel.removePhoto(uri) },
            )

            viewModel.error?.let { err ->
                Text(
                    text = err.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = { viewModel.submit() },
            enabled = viewModel.canSubmit,
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
                    text = stringResource(R.string.add_equipment_submit),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: EquipmentCategory?,
    onSelect: (EquipmentCategory) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val display = selected?.label() ?: stringResource(R.string.equipment_select_category)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.equipment_field_category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            EquipmentCategory.entries.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.label()) },
                    onClick = {
                        onSelect(cat)
                        expanded = false
                    },
                )
            }
        }
    }
}
