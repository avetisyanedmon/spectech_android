package com.spectech.features.garage.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.spectech.domain.enums.DepositStatus
import com.spectech.domain.enums.EquipmentStatus
import com.spectech.features.garage.R
import com.spectech.uikit.components.EquipmentStatusBadge
import com.spectech.uikit.theme.SuccessGreen
import com.spectech.uikit.theme.WarningAmber

/**
 * First photo of an equipment unit (or a placeholder). Overlays two
 * top-right badges, stacked vertically in iOS order
 * (SpecTechIOS/Scene/Tabs/Garage/Equipment/EquipmentCardView.swift:23-37):
 *
 *   1. [EquipmentStatusBadge] for the parsed `Status:` value
 *   2. The "Обеспечение" (deposit) badge when the unit's deposit is PAID
 *
 * Both badges are optional — pass `null` to skip either. The legacy "deposit
 * pending" amber badge is preserved for parity with the previous Android
 * behaviour even though iOS only renders the paid one.
 */
@Composable
fun EquipmentHeroImage(
    photoUrl: String?,
    depositStatus: DepositStatus?,
    modifier: Modifier = Modifier,
    equipmentStatus: EquipmentStatus? = null,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
    ) {
        if (photoUrl.isNullOrEmpty()) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.matchParentSize(),
                ) {}
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }

        // Top-right badge stack — status first (matches iOS layout), then
        // the deposit pill.
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            EquipmentStatusBadge(status = equipmentStatus)

            when (depositStatus) {
                DepositStatus.PAID -> DepositBadge(
                    text = androidx.compose.ui.res.stringResource(R.string.deposit_paid),
                    color = SuccessGreen,
                )
                DepositStatus.PENDING -> DepositBadge(
                    text = androidx.compose.ui.res.stringResource(R.string.deposit_pending),
                    color = WarningAmber,
                )
                else -> Unit
            }
        }
    }
}

@Composable
private fun DepositBadge(text: String, color: Color) {
    Surface(
        color = color,
        contentColor = Color.White,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
