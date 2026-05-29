package com.spectech.features.garage.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spectech.domain.model.Equipment
import com.spectech.domain.parsing.EquipmentCharacteristics
import com.spectech.uikit.strings.label

/**
 * One row in the garage list. Mirrors iOS `EquipmentCardView`
 * (SpecTechIOS/Scene/Tabs/Garage/Equipment/EquipmentCardView.swift):
 *
 *   ┌──────────────────────────────────────────────────────┐
 *   │                                          [• Status]  │
 *   │           [hero image]                                │
 *   │                                       [✓ Обеспечение] │
 *   ├──────────────────────────────────────────────────────┤
 *   │  Excavator                                            │
 *   │  Komatsu PC200                                        │
 *   │  # ABC123XYZ                                          │
 *   │  📍 Moscow                                            │
 *   └──────────────────────────────────────────────────────┘
 *
 * Status / VIN / City parsed out of the free-text
 * [Equipment.characteristics] blob the Add-Equipment form writes.
 */
@Composable
fun EquipmentCard(
    equipment: Equipment,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val raw = equipment.characteristics
    val status = remember(raw) { EquipmentCharacteristics.status(raw) }
    val vin = remember(raw) { EquipmentCharacteristics.vin(raw) }
    val city = remember(raw) { EquipmentCharacteristics.city(raw) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EquipmentHeroImage(
                photoUrl = equipment.photos.firstOrNull(),
                depositStatus = equipment.depositStatus,
                equipmentStatus = status,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f),
            )
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                equipment.category?.let { cat ->
                    Text(
                        text = cat.label(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = equipment.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                vin?.let { LabeledRow(icon = Icons.Outlined.Numbers, text = it) }
                city?.let { LabeledRow(icon = Icons.Outlined.LocationOn, text = it) }
            }
        }
    }
}

@Composable
private fun LabeledRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
