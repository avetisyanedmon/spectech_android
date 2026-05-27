package com.spectech.features.garage.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PhotoGrid(
    existingPhotoUrls: List<String> = emptyList(),
    pendingPhotoUris: List<Uri> = emptyList(),
    maxCount: Int,
    addLabel: String,
    onAdd: (List<Uri>) -> Unit,
    onRemoveExisting: ((String) -> Unit)? = null,
    onRemovePending: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = maxCount.coerceAtLeast(2),
        ),
    ) { uris ->
        if (uris.isNotEmpty()) onAdd(uris)
    }

    val totalCount = existingPhotoUrls.size + pendingPhotoUris.size
    val canAddMore = totalCount < maxCount

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        existingPhotoUrls.forEach { url ->
            PhotoTile(
                model = url,
                onRemove = onRemoveExisting?.let { remove -> { remove(url) } },
            )
        }
        pendingPhotoUris.forEach { uri ->
            PhotoTile(
                model = uri,
                onRemove = { onRemovePending(uri) },
            )
        }
        if (canAddMore) {
            AddTile(
                label = addLabel,
                onClick = {
                    launcher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                        )
                    )
                },
            )
        }
    }
}

@Composable
private fun PhotoTile(model: Any, onRemove: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        onRemove?.let { remove ->
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .clickable(onClick = remove),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun AddTile(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
