package com.spectech.features.garage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.spectech.domain.model.Equipment
import com.spectech.domain.state.RemoteState
import com.spectech.features.garage.R
import com.spectech.features.garage.deposit.DepositInfoSheet
import com.spectech.features.garage.ui.components.EquipmentHeroImage
import com.spectech.features.garage.viewmodel.GarageViewModel
import com.spectech.uikit.components.LoadingStateView
import com.spectech.uikit.strings.label

@Composable
fun EquipmentDetailScreen(
    equipmentId: String,
    onClose: () -> Unit,
    paddingValues: PaddingValues = PaddingValues(),
    viewModel: GarageViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val equipment: Equipment? = when (val s = state) {
        is RemoteState.Loaded -> s.value.firstOrNull { it.id == equipmentId }
        else -> null
    }

    if (equipment == null) {
        LoadingStateView(
            title = stringResource(com.spectech.uikit.R.string.state_loading),
            paddingValues = paddingValues,
        )
        return
    }

    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDepositSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PhotoCarousel(photos = equipment.photos, depositStatus = equipment.depositStatus)

        HeaderCard(equipment)
        CharacteristicsCard(equipment)
        equipment.additionalEquipment?.takeIf { it.isNotBlank() }?.let { extras ->
            AdditionalEquipmentCard(extras)
        }

        // Performance deposit is the gateway to bidding; primary action above
        // Edit/Delete because it changes the equipment's eligibility.
        Button(
            onClick = { showDepositSheet = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(
                text = stringResource(R.string.equipment_action_deposit),
                color = Color.White,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { showEditSheet = true },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(50),
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
                Spacer(Modifier.padding(start = 8.dp))
                Text(stringResource(R.string.equipment_action_edit))
            }
            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.White)
                Spacer(Modifier.padding(start = 8.dp))
                Text(stringResource(R.string.equipment_action_delete), color = Color.White)
            }
        }

        viewModel.deleteError?.let { err ->
            Text(
                text = err.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showEditSheet) {
        EditEquipmentSheet(
            equipment = equipment,
            onDismiss = { showEditSheet = false },
        )
    }

    if (showDepositSheet) {
        DepositInfoSheet(
            equipmentId = equipment.id,
            onDismiss = { showDepositSheet = false },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.equipment_delete_confirm_title)) },
            text = { Text(stringResource(R.string.equipment_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteEquipment(equipment.id) { onClose() }
                }) {
                    Text(
                        text = stringResource(R.string.equipment_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.equipment_delete_cancel))
                }
            },
        )
    }
}

@Composable
private fun PhotoCarousel(photos: List<String>, depositStatus: com.spectech.domain.enums.DepositStatus?) {
    if (photos.isEmpty()) {
        EquipmentHeroImage(
            photoUrl = null,
            depositStatus = depositStatus,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f),
        )
        return
    }
    val pagerState = rememberPagerState(pageCount = { photos.size })
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(12.dp)),
    ) { index ->
        AsyncImage(
            model = photos[index],
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun HeaderCard(equipment: Equipment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = equipment.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            equipment.category?.let { cat ->
                Text(
                    text = cat.label(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CharacteristicsCard(equipment: Equipment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.equipment_section_characteristics),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = equipment.characteristics.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.equipment_no_characteristics),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AdditionalEquipmentCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.equipment_section_additional),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
