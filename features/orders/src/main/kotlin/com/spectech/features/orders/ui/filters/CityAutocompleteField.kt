package com.spectech.features.orders.ui.filters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.spectech.domain.util.CitySuggestion
import com.spectech.domain.util.RegionCities
import com.spectech.features.orders.R

/**
 * Inline autocomplete for the City filter. Typing filters the static
 * [RegionCities] suggestions scoped to the selected regions; a city can only
 * be committed by tapping a suggestion in the dropdown — typed text is never
 * stored as a filter value and is cleared whenever the field loses focus
 * (keyboard Done, outside tap) without a selection. Selected cities render
 * as removable chips below the field, preserving the multi-select semantics
 * of `OrderFilters.selectedCities`.
 *
 * The field mirrors the previous full-screen picker's disabled-until-a-region
 * -is-chosen affordance.
 */
@Composable
fun CityAutocompleteField(
    selection: Set<String>,
    regions: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    var fieldWidthPx by remember { mutableIntStateOf(0) }
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val enabled = regions.isNotEmpty()

    // Changing the region scope invalidates any in-progress typing.
    LaunchedEffect(regions) { query = "" }

    val allCities = remember(regions) { RegionCities.topCities(regions) }
    val suggestions = remember(query, allCities, selection) {
        val trimmed = query.trim().lowercase()
        allCities
            .asSequence()
            .filter { it.name !in selection }
            .filter { trimmed.isEmpty() || it.name.lowercase().contains(trimmed) }
            .take(MAX_SUGGESTIONS)
            .toList()
    }

    val expanded = enabled && isFocused && (suggestions.isNotEmpty() || query.isNotBlank())

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                enabled = enabled,
                label = { Text(stringResource(R.string.filters_row_city)) },
                placeholder = { Text(stringResource(R.string.filters_city_picker_search)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { fieldWidthPx = it.size.width }
                    .onFocusChanged { state ->
                        isFocused = state.isFocused
                        // Closed without picking a suggestion — discard the text.
                        if (!state.isFocused) query = ""
                    },
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { focusManager.clearFocus() },
                // Keep the popup non-focusable so the keyboard stays up and
                // typing keeps flowing into the text field while the
                // suggestions are visible.
                properties = PopupProperties(focusable = false),
                modifier = Modifier
                    .width(with(density) { fieldWidthPx.toDp() })
                    .heightIn(max = SUGGESTIONS_MAX_HEIGHT),
            ) {
                if (suggestions.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.filters_city_picker_empty_title)) },
                        onClick = {},
                        enabled = false,
                    )
                } else {
                    suggestions.forEach { city ->
                        SuggestionItem(
                            city = city,
                            onClick = {
                                onSelectionChange(selection + city.name)
                                query = ""
                            },
                        )
                    }
                }
            }
        }

        if (selection.isNotEmpty()) {
            SelectedCityChips(selection = selection, onSelectionChange = onSelectionChange)
        }
    }
}

@Composable
private fun SuggestionItem(city: CitySuggestion, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Column {
                Text(text = city.name, style = MaterialTheme.typography.bodyLarge)
                city.subtitle?.takeIf { it.isNotBlank() }?.let { sub ->
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        onClick = onClick,
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SelectedCityChips(
    selection: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        selection.sorted().forEach { city ->
            InputChip(
                selected = true,
                onClick = { onSelectionChange(selection - city) },
                label = { Text(city) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.filters_city_remove, city),
                        modifier = Modifier.size(InputChipDefaults.IconSize),
                    )
                },
            )
        }
    }
}

/** DropdownMenu lays its items out eagerly, so cap how many we offer at once. */
private const val MAX_SUGGESTIONS = 25

private val SUGGESTIONS_MAX_HEIGHT = 280.dp
