package com.spectech.features.garage.ui.components

import androidx.compose.foundation.layout.Box
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
import com.spectech.features.garage.R
import com.spectech.uikit.theme.SuccessGreen
import com.spectech.uikit.theme.WarningAmber

/**
 * First photo of an equipment unit (or a placeholder). Optional deposit
 * status badge overlay matches iOS `EquipmentHeroImage`. Phase 10 will hook
 * the badge into the actual deposit flow; for Phase 8 we still render it
 * because the list endpoint includes `depositStatus` on each row.
 */
@Composable
fun EquipmentHeroImage(
    photoUrl: String?,
    depositStatus: DepositStatus?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
    ) {
        if (photoUrl.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .matchParentSize(),
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

@Composable
private fun BoxScope.DepositBadge(text: String, color: Color) = with(this) {
    Surface(
        color = color,
        contentColor = Color.White,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Suppress("unused")
private typealias BoxScope = androidx.compose.foundation.layout.BoxScope
