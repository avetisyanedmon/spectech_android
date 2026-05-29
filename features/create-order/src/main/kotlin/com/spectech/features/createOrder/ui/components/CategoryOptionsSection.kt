package com.spectech.features.createOrder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spectech.domain.enums.EquipmentCategory
import com.spectech.features.createOrder.R
import com.spectech.features.createOrder.viewmodel.CategoryOptionsState
import com.spectech.uikit.theme.WarningAmber as WarningAmberColor

/**
 * Per-category optional parameters block. Mirrors iOS
 * `CategoryOptionsSection` (SpecTechIOS/Features/CreateOrder/CreateOrderView.swift:571-828)
 * branch-for-branch — same field set, same Russian chip labels, same warning
 * banner on the dump-truck inert-material field.
 *
 * Skips rendering entirely for categories listed in
 * [CategoryOptionsState.CATEGORIES_WITHOUT_OPTIONS] so the form doesn't show
 * an empty section card.
 */
@Composable
fun CategoryOptionsSection(
    category: EquipmentCategory,
    options: CategoryOptionsState,
    modifier: Modifier = Modifier,
) {
    if (category in CategoryOptionsState.CATEGORIES_WITHOUT_OPTIONS) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                RoundedCornerShape(12.dp),
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(12.dp),
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.create_order_additional_parameters),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.create_order_optional),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
        CategoryContent(category = category, options = options)
    }
}

@Composable
private fun CategoryContent(category: EquipmentCategory, options: CategoryOptionsState) {
    when (category) {
        EquipmentCategory.CONCRETE_PUMP ->
            NumberField(
                label = stringResource(R.string.opt_boom_length_m),
                value = options.boomLength,
                onChange = { options.boomLength = filterDecimal(it) },
            )

        EquipmentCategory.AERIAL_PLATFORM ->
            NumberField(
                label = stringResource(R.string.opt_boom_reach_m),
                value = options.boomLength,
                onChange = { options.boomLength = filterDecimal(it) },
            )

        EquipmentCategory.TRUCK_CRANE, EquipmentCategory.MANIPULATOR -> {
            NumberField(
                label = stringResource(R.string.opt_lift_capacity_t),
                value = options.liftCapacity,
                onChange = { options.liftCapacity = filterDecimal(it) },
            )
            NumberField(
                label = stringResource(R.string.opt_boom_length_m),
                value = options.boomLength,
                onChange = { options.boomLength = filterDecimal(it) },
            )
        }

        EquipmentCategory.SEWAGE_VACUUM ->
            ChipPickerOptional(
                label = stringResource(R.string.opt_type),
                options = listOf("Ассенизатор", "Илосос"),
                selection = options.sewageType,
                onSelect = { options.sewageType = it },
            )

        EquipmentCategory.FUEL_TANKER ->
            ChipPickerOptional(
                label = stringResource(R.string.opt_type),
                options = listOf("Бензовоз", "Автоцистерна"),
                selection = options.fuelTankerType,
                onSelect = { options.fuelTankerType = it },
            )

        EquipmentCategory.GRADER ->
            ChipPickerOptional(
                label = stringResource(R.string.opt_size),
                options = listOf("Маленький", "Средний", "Большой"),
                selection = options.graderSize,
                onSelect = { options.graderSize = it },
            )

        EquipmentCategory.ASPHALT_PAVER ->
            ChipPickerOptional(
                label = stringResource(R.string.opt_size),
                options = listOf("Маленький", "Средний", "Большой"),
                selection = options.asphaltPaverSize,
                onSelect = { options.asphaltPaverSize = it },
            )

        EquipmentCategory.ROAD_ROLLER -> {
            ChipPickerOptional(
                label = stringResource(R.string.opt_drum_type),
                options = listOf("Гладковальцовый", "Пневмоколёсный", "Комбинированный"),
                selection = options.roadRollerType,
                onSelect = { options.roadRollerType = it },
            )
            NumberField(
                label = stringResource(R.string.opt_tonnage_t),
                value = options.tonnage,
                onChange = { options.tonnage = filterDecimal(it) },
            )
        }

        EquipmentCategory.SOIL_COMPACTOR ->
            ChipPickerOptional(
                label = stringResource(R.string.opt_drum_type),
                options = listOf("Гладковальцовый", "Кулачковый"),
                selection = options.soilCompactorType,
                onSelect = { options.soilCompactorType = it },
            )

        EquipmentCategory.MINI_LOADER -> {
            ChipPickerOptional(
                label = stringResource(R.string.opt_chassis),
                options = listOf("Гусеничный", "Колёсный"),
                selection = options.miniLoaderBase,
                onSelect = { options.miniLoaderBase = it },
            )
            MultiSelectChecklist(
                label = stringResource(R.string.opt_attachments),
                options = listOf("Гидромолот", "Гидробур", "Паллетные вилы", "Дорожная фреза", "Снежный отвал"),
                selected = options.miniLoaderAttachments.toSet(),
                onToggle = options::toggleMiniLoaderAttachment,
            )
        }

        EquipmentCategory.MINI_EXCAVATOR ->
            NumberField(
                label = stringResource(R.string.opt_tonnage_range_1_6_t),
                value = options.tonnage,
                onChange = { options.tonnage = filterDecimal(it) },
            )

        EquipmentCategory.GARBAGE_TRUCK ->
            ChipPickerOptional(
                label = stringResource(R.string.opt_type),
                options = listOf("Мусоровоз", "Бункеровоз", "Ломовоз"),
                selection = options.garbageTruckType,
                onSelect = { options.garbageTruckType = it },
            )

        EquipmentCategory.LOWBED_TRAILER ->
            ChipPickerOptional(
                label = stringResource(R.string.opt_type),
                options = listOf("Трал низкорамный", "Платформа"),
                selection = options.lowbedType,
                onSelect = { options.lowbedType = it },
            )

        EquipmentCategory.DUMP_TRUCK -> {
            ChipPickerOptional(
                label = stringResource(R.string.opt_axle_configuration),
                options = listOf("Двухосный", "Трёхосный", "Четырёхосный", "Полуприцеп", "Прицеп"),
                selection = options.dumpTruckAxles,
                onSelect = { options.dumpTruckAxles = it },
            )
            InertMaterialField(options = options)
        }

        EquipmentCategory.FRONT_LOADER ->
            ChipPickerOptional(
                label = stringResource(R.string.opt_bucket_volume_m3),
                options = listOf("1", "2", "3", "4", "5"),
                selection = options.frontLoaderBucket,
                onSelect = { options.frontLoaderBucket = it },
            )

        EquipmentCategory.TOW_TRUCK ->
            ChipPickerOptional(
                label = stringResource(R.string.opt_type),
                options = listOf("Эвакуатор", "Автовоз"),
                selection = options.towType,
                onSelect = { options.towType = it },
            )

        EquipmentCategory.EXCAVATOR_CRAWLER, EquipmentCategory.EXCAVATOR_WHEELED ->
            ChipPickerOptional(
                label = stringResource(R.string.opt_bucket_volume_m3),
                options = listOf("0.8", "1", "2", "3", "4", "5"),
                selection = options.excavatorBucket,
                onSelect = { options.excavatorBucket = it },
            )

        EquipmentCategory.AUGER_DRILL -> {
            NumberField(
                label = stringResource(R.string.opt_drilling_depth_m),
                value = options.drillingDepth,
                onChange = { options.drillingDepth = filterDecimal(it) },
            )
            NumberField(
                label = stringResource(R.string.opt_diameter_mm),
                value = options.drillingDiameter,
                onChange = { options.drillingDiameter = filterDecimal(it) },
            )
        }

        EquipmentCategory.BACKHOE_LOADER -> {
            MultiSelectChecklist(
                label = stringResource(R.string.opt_attachments),
                options = listOf("Гидромолот", "Гидробур", "Вибропогружатель", "Паллетные вилы", "Дорожная фреза", "Снежный отвал"),
                selected = options.backhoeAttachments.toSet(),
                onToggle = options::toggleBackhoeAttachment,
            )
            ChipPickerOptional(
                label = stringResource(R.string.opt_bucket_width_cm),
                options = listOf("30", "40", "60", "80", "100"),
                selection = options.backhoeBucketWidth,
                onSelect = { options.backhoeBucketWidth = it },
            )
        }

        EquipmentCategory.BITUMEN_SPRAYER ->
            NumberField(
                label = stringResource(R.string.opt_tank_volume_m3),
                value = options.volumeCubic,
                onChange = { options.volumeCubic = filterDecimal(it) },
            )

        EquipmentCategory.FORKLIFT ->
            NumberField(
                label = stringResource(R.string.opt_lift_capacity_max_15_t),
                value = options.liftCapacity,
                onChange = { options.liftCapacity = filterDecimal(it) },
            )

        EquipmentCategory.CONCRETE_MIXER,
        EquipmentCategory.BULLDOZER,
        EquipmentCategory.GRAB_LOADER,
        EquipmentCategory.ROAD_MAINTENANCE_VEHICLE -> Unit
    }
}

// ─── reusable controls ─────────────────────────────────────────────────────

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipPickerOptional(
    label: String,
    options: List<String>,
    selection: String?,
    onSelect: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val isSelected = option == selection
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface,
                            shape = CircleShape,
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape,
                        )
                        .clickable { onSelect(if (isSelected) null else option) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun MultiSelectChecklist(
    label: String,
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { option ->
                val isOn = selected.contains(option)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(option) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = if (isOn) Icons.Filled.CheckBox
                        else Icons.Outlined.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = if (isOn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun InertMaterialField(options: CategoryOptionsState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.opt_inert_material_optional),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = options.inertMaterial,
            onValueChange = { options.inertMaterial = it },
            placeholder = { Text(stringResource(R.string.opt_inert_material_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (options.inertMaterial.trim().isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = WarningAmberColor,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(R.string.opt_inert_material_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Strips everything except digits, dots and commas. Comma → dot is left for
 * `buildSummary` so the field still displays the user's preferred separator.
 * Multiple decimal marks are not de-duped — this is a permissive filter, not
 * a parser.
 */
private fun filterDecimal(input: String): String =
    input.filter { it.isDigit() || it == '.' || it == ',' }
