package com.spectech.features.marketplace.filters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Bottom-pinned "Reset" chip used inside the multi-select region and city
 * pickers. Mirrors iOS' `.safeAreaInset(edge: .bottom)` red Reset capsule
 * (SpecTechIOS/Scene/Tabs/Marketplace/Filters/MarketplaceFilterSheet.swift).
 */
@Composable
fun ResetBar(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(
            onClick = onClick,
            shape = CircleShape,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = CircleShape,
                )
                .padding(horizontal = 24.dp),
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
