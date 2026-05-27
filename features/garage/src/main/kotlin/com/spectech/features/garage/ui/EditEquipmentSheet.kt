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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spectech.domain.model.Equipment
import com.spectech.features.garage.R
import com.spectech.features.garage.ui.components.PhotoGrid
import com.spectech.features.garage.viewmodel.EditEquipmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEquipmentSheet(
    equipment: Equipment,
    onDismiss: () -> Unit,
    viewModel: EditEquipmentViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(equipment.id) {
        viewModel.seedFrom(equipment)
    }

    LaunchedEffect(viewModel.success) {
        if (viewModel.success) {
            viewModel.reset()
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        EditEquipmentForm(viewModel = viewModel, onClose = onDismiss)
    }
}

@Composable
private fun EditEquipmentForm(viewModel: EditEquipmentViewModel, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.edit_equipment_title),
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
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = { viewModel.name = it },
                label = { Text(stringResource(R.string.equipment_field_name)) },
                singleLine = true,
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

            Text(
                text = stringResource(R.string.equipment_field_photos),
                style = MaterialTheme.typography.titleSmall,
            )
            PhotoGrid(
                existingPhotoUrls = viewModel.existingPhotoUrls.toList(),
                pendingPhotoUris = viewModel.pendingPhotos.toList(),
                maxCount = EditEquipmentViewModel.MAX_PHOTOS,
                addLabel = stringResource(R.string.equipment_add_photo),
                onAdd = { uris -> viewModel.addPhotos(uris) },
                onRemoveExisting = { url -> viewModel.removeExistingPhoto(url) },
                onRemovePending = { uri -> viewModel.removePendingPhoto(uri) },
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
                    text = stringResource(R.string.edit_equipment_submit),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
        }
    }
}
