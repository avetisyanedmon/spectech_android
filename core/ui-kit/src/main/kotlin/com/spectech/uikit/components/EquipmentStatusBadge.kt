package com.spectech.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spectech.domain.enums.EquipmentStatus
import com.spectech.uikit.R
import com.spectech.uikit.theme.DestructiveRed
import com.spectech.uikit.theme.SuccessGreen
import com.spectech.uikit.theme.WarningAmber

/**
 * Small status pill rendered over an [EquipmentHeroImage] on the garage card
 * and at the top of the equipment detail screen. Mirrors iOS
 * `EquipmentStatusBadge` (SpecTechIOS/Shared/Views/EquipmentHeroImage.swift:70-102)
 * — same three-colour palette, same Russian / English copy.
 *
 * The badge is stateless — the caller passes the resolved [EquipmentStatus]
 * (or `null` to skip rendering). `null` returns nothing so an equipment unit
 * without a parsable `Status:` field simply has no badge instead of a
 * grey-on-grey placeholder.
 */
@Composable
fun EquipmentStatusBadge(
    status: EquipmentStatus?,
    modifier: Modifier = Modifier,
) {
    val resolved = status ?: return
    val (tint, label) = statusVisuals(resolved)
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = tint,
        modifier = modifier
            .background(tint.copy(alpha = 0.15f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun statusVisuals(status: EquipmentStatus): Pair<Color, String> = when (status) {
    EquipmentStatus.AVAILABLE ->
        SuccessGreen to stringResource(R.string.equipment_status_available)
    EquipmentStatus.IN_USE ->
        DestructiveRed to stringResource(R.string.equipment_status_in_use)
    EquipmentStatus.MAINTENANCE ->
        WarningAmber to stringResource(R.string.equipment_status_maintenance)
}
