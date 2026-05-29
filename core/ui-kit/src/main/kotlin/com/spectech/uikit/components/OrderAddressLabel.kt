package com.spectech.uikit.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.spectech.uikit.theme.SuccessGreen
import kotlinx.coroutines.delay

/**
 * One-line address pill with map-pin icon and tap-to-copy button. Mirrors iOS
 * `OrderAddressLabel` (SpecTechIOS/Shared/Views/OrderAddressLabel.swift).
 *
 * Joins city + street as `"city, street"`, falls back gracefully when either
 * side is blank. Callers pass [copyContentDescription] (typically the
 * `R.string.detail_copy_address` / `marketplace_copy_address` resource) so
 * this component stays free of any module's resource graph.
 *
 *   ┌───────────────────────────────────────────────────┐
 *   │  📍  Moscow, Lenin Prospect 12                 ⧉  │
 *   └───────────────────────────────────────────────────┘
 *
 * Used on the marketplace card, order detail, and bid rows.
 */
@Composable
fun OrderAddressLabel(
    city: String,
    address: String?,
    copyContentDescription: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val fullText = remember(city, address) { joinCityAndAddress(city, address) }
    var copied by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    // Tiny scale bump on copy success so the checkmark feels intentional.
    val checkScale by animateFloatAsState(
        targetValue = if (copied) 1.1f else 1f,
        label = "copy-bump",
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = fullText,
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
        )
        Spacer(Modifier.size(4.dp))
        IconButton(
            onClick = {
                clipboard.setText(AnnotatedString(fullText))
                copied = true
            },
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                contentDescription = copyContentDescription,
                tint = if (copied) SuccessGreen else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size((16f * checkScale).dp),
            )
        }
    }
}

/**
 * Joins city and address into `"city, address"`, falling back to whichever side
 * is non-empty. Matches the formatting iOS uses in
 * `OrderAddressLabel.fullText`.
 */
private fun joinCityAndAddress(city: String, address: String?): String {
    val a = address?.trim().orEmpty()
    val c = city.trim()
    return when {
        a.isEmpty() -> c
        c.isEmpty() -> a
        else -> "$c, $a"
    }
}
