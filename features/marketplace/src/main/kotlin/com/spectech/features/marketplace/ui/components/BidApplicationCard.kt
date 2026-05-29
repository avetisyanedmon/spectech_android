package com.spectech.features.marketplace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.spectech.domain.enums.PricingUnit
import com.spectech.domain.model.Bid
import com.spectech.domain.model.ContractorContact
import com.spectech.features.marketplace.R
import com.spectech.uikit.components.PhoneActionButton
import com.spectech.uikit.strings.label
import java.text.NumberFormat
import java.util.Locale

/**
 * One row in the "Received Applications" list on the customer-side order
 * detail. Mirrors iOS `BidApplicationCard`
 * (SpecTechIOS/Scene/Tabs/Marketplace/Order/OrderDetailView.swift:679-946).
 *
 *   ┌──────────────────────────────────────────────────────────────┐
 *   │  [photo strip — horizontal scroll, tap for full-screen zoom] │
 *   │                                                              │
 *   │  Category title                                              │
 *   │  Make · Model · Year                                         │
 *   │  Description (parsed from characteristics)                   │
 *   │  ─────────────────────                                       │
 *   │  Unit Price (per hour)        Delivery Price                 │
 *   │     1 200 ₽                      500 ₽                       │
 *   │  ─────────────────────                                       │
 *   │  💬 Comment                                                  │
 *   │  [Accept Offer]  →  ✔ Offer accepted  + Contact card         │
 *   └──────────────────────────────────────────────────────────────┘
 */
@Composable
fun BidApplicationCard(
    bid: Bid,
    pricingUnit: PricingUnit?,
    isOwnOrder: Boolean,
    isAccepted: Boolean,
    isAccepting: Boolean,
    acceptedContact: ContractorContact?,
    onAccept: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var zoomedPhoto by remember { mutableStateOf<String?>(null) }
    val photos = bid.equipmentPhotos.orEmpty()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            if (photos.isNotEmpty()) {
                PhotoStrip(
                    photos = photos,
                    onPhotoTap = { zoomedPhoto = it },
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = if (photos.isEmpty()) 16.dp else 0.dp,
                        bottom = 16.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EquipmentInfo(bid = bid)
                EquipmentDescription(bid = bid)
                HorizontalDivider()
                PricingRow(bid = bid, pricingUnit = pricingUnit)
                CommentRow(bid = bid)
                if (isOwnOrder) {
                    HorizontalDivider()
                    if (isAccepted) {
                        AcceptedBanner()
                        Spacer(Modifier.height(4.dp))
                        ContactInformationCard(contact = acceptedContact)
                    } else if (onAccept != null) {
                        AcceptOfferButton(
                            onClick = onAccept,
                            isAccepting = isAccepting,
                        )
                    }
                }
            }
        }
    }

    zoomedPhoto?.let { photo ->
        ZoomablePhotoViewer(photo = photo, onDismiss = { zoomedPhoto = null })
    }
}

// ─── photo strip ───────────────────────────────────────────────────────────

@Composable
private fun PhotoStrip(photos: List<String>, onPhotoTap: (String) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items = photos, key = { it.hashCode() }) { photo ->
            BidEquipmentPhoto(photo = photo, onClick = { onPhotoTap(photo) })
        }
    }
}

@Composable
private fun BidEquipmentPhoto(photo: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 130.dp, height = 100.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
    ) {
        val isUrl = photo.startsWith("http", ignoreCase = true)
        if (isUrl) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(photo).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth(),
                loading = { Box(Modifier.fillMaxWidth(), Alignment.Center) { CircularProgressIndicator() } },
                error = {
                    Box(Modifier.fillMaxWidth(), Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.BrokenImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        } else {
            // base64 — defer decoding to ZoomablePhotoViewer when tapped.
            Icon(
                imageVector = Icons.Outlined.BrokenImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

// ─── equipment info block ───────────────────────────────────────────────────

@Composable
private fun EquipmentInfo(bid: Bid) {
    val context = LocalContext.current
    val category = bid.equipmentCategory?.label(context) ?: bid.equipmentName ?: stringResource(R.string.detail_no_category)
    val make = bid.equipmentName?.substringBefore(" ", "—") ?: "—"
    val model = bid.equipmentName ?: "—"
    val year = parseCharacteristic(bid, "Year", "Год") ?: parseAdditionalInfo(bid, "Year", "Год") ?: "—"

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = category,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoChip(label = stringResource(R.string.bid_card_make), value = make)
            InfoChip(label = stringResource(R.string.bid_card_model), value = model)
        }
        InfoChip(label = stringResource(R.string.bid_card_year), value = year)
    }
}

@Composable
private fun EquipmentDescription(bid: Bid) {
    val desc = parseCharacteristic(bid, "Description", "Описание") ?: return
    if (desc.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.bid_card_description),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ─── pricing row ───────────────────────────────────────────────────────────

@Composable
private fun PricingRow(bid: Bid, pricingUnit: PricingUnit?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
            .padding(vertical = 10.dp),
    ) {
        val unitLabel = pricingUnit?.label()
        val unitTitle = stringResource(R.string.bid_card_unit_price) +
            if (unitLabel != null) " ($unitLabel)" else ""
        PriceCell(
            modifier = Modifier.weight(1f),
            title = unitTitle,
            value = formatCurrency(bid.price.toDouble()),
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        PriceCell(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.bid_card_delivery_price),
            value = formatCurrency((bid.deliveryPrice ?: java.math.BigDecimal.ZERO).toDouble()),
        )
    }
}

@Composable
private fun PriceCell(modifier: Modifier = Modifier, title: String, value: String) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// ─── comment ───────────────────────────────────────────────────────────────

@Composable
private fun CommentRow(bid: Bid) {
    val comment = bid.comment?.takeIf { it.isNotBlank() } ?: return
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = comment,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── accept / accepted ───────────────────────────────────────────────────

@Composable
private fun AcceptOfferButton(onClick: () -> Unit, isAccepting: Boolean) {
    Button(
        onClick = onClick,
        enabled = !isAccepting,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = com.spectech.uikit.theme.SuccessGreen,
            disabledContainerColor = com.spectech.uikit.theme.SuccessGreen.copy(alpha = 0.6f),
        ),
    ) {
        if (isAccepting) {
            CircularProgressIndicator(
                color = androidx.compose.ui.graphics.Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(
                text = stringResource(R.string.bid_card_accept_offer),
                fontWeight = FontWeight.SemiBold,
                color = androidx.compose.ui.graphics.Color.White,
            )
        }
    }
}

@Composable
private fun AcceptedBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(com.spectech.uikit.theme.SuccessGreen.copy(alpha = 0.1f))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = com.spectech.uikit.theme.SuccessGreen,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.bid_card_offer_accepted),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = com.spectech.uikit.theme.SuccessGreen,
        )
    }
}

// ─── contact reveal ────────────────────────────────────────────────────────

@Composable
private fun ContactInformationCard(contact: ContractorContact?) {
    val brandBlue = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(brandBlue.copy(alpha = 0.06f))
            .border(1.dp, brandBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.bid_card_contact_information),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        if (contact == null) {
            // Optimistic-load lag — keep the user informed instead of going silent.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.bid_card_contact_awaiting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            contact.name?.takeIf { it.isNotBlank() }?.let { name ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(text = name, style = MaterialTheme.typography.bodyMedium)
                }
            }
            val phone = contact.phone?.takeIf { it.isNotBlank() }
            if (phone != null) {
                PhoneActionButton(
                    phone = phone,
                    callContentDescription = stringResource(com.spectech.uikit.R.string.state_action_call),
                    copyContentDescription = stringResource(com.spectech.uikit.R.string.state_action_copy),
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = com.spectech.uikit.theme.WarningAmber,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.bid_card_contact_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = com.spectech.uikit.theme.WarningAmber,
                    )
                }
            }
        }
    }
}

// ─── parsing helpers ────────────────────────────────────────────────────────

/**
 * Parses a "Key: value" entry out of the " | "-separated `equipmentCharacteristics`
 * blob that GarageViews writes — e.g. "VIN: 123 | Year: 2019 | Description: ...".
 * Tries every alias in order so RU + EN labels both resolve.
 */
private fun parseCharacteristic(bid: Bid, vararg keys: String): String? {
    val raw = bid.equipmentCharacteristics?.takeIf { it.isNotBlank() } ?: return null
    for (part in raw.split("|")) {
        val trimmed = part.trim()
        val sepIdx = trimmed.indexOf(':')
        if (sepIdx <= 0) continue
        val key = trimmed.substring(0, sepIdx).trim()
        if (keys.any { it.equals(key, ignoreCase = true) }) {
            return trimmed.substring(sepIdx + 1).trim()
        }
    }
    return null
}

/** Legacy semicolon-separated fallback for the older `equipmentAdditionalInfo` blob. */
private fun parseAdditionalInfo(bid: Bid, vararg keys: String): String? {
    val raw = bid.equipmentAdditionalInfo?.takeIf { it.isNotBlank() } ?: return null
    for (part in raw.split(";")) {
        val sepIdx = part.indexOf(':')
        if (sepIdx <= 0) continue
        val key = part.substring(0, sepIdx).trim()
        if (keys.any { it.equals(key, ignoreCase = true) }) {
            return part.substring(sepIdx + 1).trim()
        }
    }
    return null
}

private fun formatCurrency(value: Double): String {
    val fmt = NumberFormat.getCurrencyInstance(Locale("ru", "RU")).apply {
        maximumFractionDigits = 0
    }
    return runCatching { fmt.format(value) }.getOrDefault("$value ₽")
}
