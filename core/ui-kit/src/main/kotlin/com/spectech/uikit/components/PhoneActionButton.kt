package com.spectech.uikit.components

import android.content.Intent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

/**
 * Inline phone number + call + copy actions. Used by My Bids (revealed
 * customer contact) and My Orders (accepted contractor contact). Mirrors iOS
 * `PhoneActionButton` (SpecTechIOS/Shared/Views/PhoneActionButton.swift).
 *
 * Caller passes the localized [callContentDescription] / [copyContentDescription]
 * so the composable stays decoupled from any module's resources.
 */
@Composable
fun PhoneActionButton(
    phone: String,
    callContentDescription: String,
    copyContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = phone,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = {
            val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
            ctx.startActivity(intent)
        }) {
            Icon(
                imageVector = Icons.Filled.Phone,
                contentDescription = callContentDescription,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = {
            clipboard.setText(AnnotatedString(phone))
        }) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = copyContentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
