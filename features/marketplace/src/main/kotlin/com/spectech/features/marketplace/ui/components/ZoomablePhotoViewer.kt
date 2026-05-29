package com.spectech.features.marketplace.ui.components

import android.util.Base64
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * Full-screen, pinch-to-zoom photo viewer. Mirrors iOS `ZoomablePhotoViewer`
 * (SpecTechIOS/Scene/Tabs/Marketplace/Order/OrderDetailView.swift:958-1072).
 *
 *   - Pinch to zoom 1×–5×.
 *   - Drag to pan once zoomed in.
 *   - Double-tap to toggle between 1× and 2.5×.
 *   - Tap the close button (top-right) or system back to dismiss.
 *
 * Accepts both HTTP URLs (loaded via Coil) and base64-encoded image data so
 * the contractor's locally-encoded preview before upload still displays.
 */
@Composable
fun ZoomablePhotoViewer(
    photo: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            ZoomableImage(photo = photo, modifier = Modifier.fillMaxSize())

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                        .padding(4.dp),
                )
            }
        }
    }
}

@Composable
private fun ZoomableImage(photo: String, modifier: Modifier = Modifier) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val animatedScale by animateFloatAsState(targetValue = scale, label = "scale")

    val transformModifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
            translationX = offsetX
            translationY = offsetY
        }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.5f
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val isUrl = photo.startsWith("http", ignoreCase = true)
        if (isUrl) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photo)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = transformModifier,
                loading = { CircularProgressIndicator(color = Color.White) },
                error = {
                    Icon(
                        imageVector = Icons.Outlined.BrokenImage,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(60.dp),
                    )
                },
            )
        } else {
            val bitmap = remember(photo) { decodeBase64(photo) }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = transformModifier,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.BrokenImage,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(60.dp),
                )
            }
        }
    }
}

/**
 * Decodes a base64-encoded image string into a [android.graphics.Bitmap].
 * Strips the optional `data:image/...;base64,` prefix so we accept both
 * RFC2397 data URLs and bare base64 strings.
 */
private fun decodeBase64(raw: String): android.graphics.Bitmap? = try {
    val cleaned = if (raw.contains(',')) raw.substringAfter(',') else raw
    val bytes = Base64.decode(cleaned, Base64.DEFAULT)
    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
} catch (_: Throwable) {
    null
}
