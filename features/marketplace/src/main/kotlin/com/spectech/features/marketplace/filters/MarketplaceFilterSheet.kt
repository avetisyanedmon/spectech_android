package com.spectech.features.marketplace.filters

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spectech.domain.enums.EquipmentCategory
import com.spectech.domain.enums.PaymentType
import com.spectech.domain.enums.PricingUnit
import com.spectech.domain.model.OrderFilters
import com.spectech.features.marketplace.R
import com.spectech.features.marketplace.savedfilter.SavedFilterSection
import com.spectech.uikit.strings.label

/**
 * Marketplace filter sheet. Mirrors iOS
 * `MarketplaceFilterSheet` (SpecTechIOS/Scene/Tabs/Marketplace/Filters/MarketplaceFilterSheet.swift)
 * across all five dimensions — equipment category, region, city, pricing
 * unit, payment type — plus the saved-filter + notifications section.
 *
 * Region and city use full-screen multi-select dialogs ([MultiRegionPickerDialog],
 * [MultiCityPickerDialog]) rather than chip groups, since 85 federal subjects
 * and hundreds of cities don't fit a horizontal chip row. The city row
 * mirrors iOS' disabled-until-a-region-is-chosen affordance and clears any
 * staged cities whenever the staged region set is mutated.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceFilterSheet(
    current: OrderFilters,
    onApply: (OrderFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(current) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showRegionPicker by remember { mutableStateOf(false) }
    var showCityPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = stringResource(R.string.filters_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            FilterSection(stringResource(R.string.filters_section_category)) {
                ChipGroup(
                    items = EquipmentCategory.entries,
                    isSelected = { it in draft.categories },
                    label = { it.label() },
                    onToggle = { cat ->
                        draft = draft.copy(
                            categories = draft.categories.toggle(cat),
                        )
                    },
                )
            }

            FilterSection(stringResource(R.string.filters_section_location)) {
                PickerRow(
                    title = stringResource(R.string.filters_row_region),
                    summary = countSummary(
                        count = draft.regions.size,
                        anyLabel = stringResource(R.string.filters_summary_any),
                        selectedLabel = stringResource(
                            R.string.filters_summary_selected,
                            draft.regions.size,
                        ),
                    ),
                    enabled = true,
                    onClick = { showRegionPicker = true },
                )
                PickerRow(
                    title = stringResource(R.string.filters_row_city),
                    summary = countSummary(
                        count = draft.selectedCities.size,
                        anyLabel = stringResource(R.string.filters_summary_any),
                        selectedLabel = stringResource(
                            R.string.filters_summary_selected,
                            draft.selectedCities.size,
                        ),
                    ),
                    enabled = draft.regions.isNotEmpty(),
                    onClick = { showCityPicker = true },
                )
            }

            FilterSection(stringResource(R.string.filters_section_pricing_unit)) {
                ChipGroup(
                    items = PricingUnit.entries,
                    isSelected = { it in draft.pricingUnits },
                    label = { it.label() },
                    onToggle = { u ->
                        draft = draft.copy(pricingUnits = draft.pricingUnits.toggle(u))
                    },
                )
            }

            FilterSection(stringResource(R.string.filters_section_payment)) {
                ChipGroup(
                    items = PaymentType.entries,
                    isSelected = { it in draft.paymentTypes },
                    label = { it.label() },
                    onToggle = { pt ->
                        draft = draft.copy(paymentTypes = draft.paymentTypes.toggle(pt))
                    },
                )
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = { draft = OrderFilters() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.filters_clear))
            }

            Button(
                onClick = {
                    onApply(draft)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(stringResource(R.string.filters_apply))
            }

            // "Save this filter + notify me on new matches" lives below the
            // primary Apply action so it doesn't compete with the main flow
            // (browse → filter → apply). It still uses [draft] so the user
            // can subscribe without first tapping Apply.
            SavedFilterSection(draft = draft)

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showRegionPicker) {
        MultiRegionPickerDialog(
            selection = draft.regions,
            onConfirm = { newRegions ->
                // Match iOS: changing the region set wipes any previously
                // staged cities so the city scope can never get out of sync
                // with the region scope.
                val cleared = if (newRegions != draft.regions) emptySet() else draft.selectedCities
                draft = draft.copy(regions = newRegions, selectedCities = cleared)
            },
            onDismiss = { showRegionPicker = false },
        )
    }

    if (showCityPicker) {
        MultiCityPickerDialog(
            selection = draft.selectedCities,
            regions = draft.regions,
            onConfirm = { newCities -> draft = draft.copy(selectedCities = newCities) },
            onDismiss = { showCityPicker = false },
        )
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipGroup(
    items: List<T>,
    isSelected: (T) -> Boolean,
    label: @Composable (T) -> String,
    onToggle: (T) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            FilterChip(
                selected = isSelected(item),
                onClick = { onToggle(item) },
                label = { Text(label(item)) },
            )
        }
    }
}

/**
 * Tap-to-open row used for Region and City. Shows the title on the left, a
 * count summary on the right ("Any" or "N selected"), and a chevron. Matches
 * iOS' NavigationLink rows in the Form-style filter sheet.
 */
@Composable
private fun PickerRow(
    title: String,
    summary: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val containerAlpha = if (enabled) 1f else 0.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = containerAlpha),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = containerAlpha),
            modifier = Modifier.padding(end = 8.dp),
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = containerAlpha),
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun countSummary(count: Int, anyLabel: String, selectedLabel: String): String =
    if (count == 0) anyLabel else selectedLabel

private fun <T> Set<T>.toggle(item: T): Set<T> =
    if (item in this) this - item else this + item
