package com.spectech.features.createOrder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spectech.domain.enums.EquipmentCategory
import com.spectech.domain.enums.PaymentType
import com.spectech.domain.enums.PricingUnit
import com.spectech.uikit.strings.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPicker(
    value: EquipmentCategory,
    onValueChange: (EquipmentCategory) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    EnumDropdown(
        value = value,
        onValueChange = onValueChange,
        items = EquipmentCategory.entries,
        renderLabel = { it.label() },
        fieldLabel = label,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingUnitPicker(
    value: PricingUnit,
    onValueChange: (PricingUnit) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    EnumDropdown(
        value = value,
        onValueChange = onValueChange,
        items = PricingUnit.entries,
        renderLabel = { it.label() },
        fieldLabel = label,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PaymentTypeChips(
    selected: PaymentType,
    onSelect: (PaymentType) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PaymentType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text(type.label()) },
            )
        }
    }
}

// Generic dropdown helper -----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    value: T,
    onValueChange: (T) -> Unit,
    items: List<T>,
    renderLabel: @Composable (T) -> String,
    fieldLabel: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val display = renderLabel(value)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(fieldLabel) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(type = androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            items.forEach { item ->
                val label = renderLabel(item)
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onValueChange(item)
                        expanded = false
                    },
                )
            }
        }
    }
}
