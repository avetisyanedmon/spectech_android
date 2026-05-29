package com.spectech.features.marketplace.filters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
 * Marketplace filter sheet (Phase 5 cut). Includes the three dimensions that
 * are pure chip-toggles — equipment category, pricing unit, and payment type.
 * Region + city autocomplete land later when Google Places is configured.
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

private fun <T> Set<T>.toggle(item: T): Set<T> =
    if (item in this) this - item else this + item
