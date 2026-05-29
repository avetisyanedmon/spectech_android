package com.spectech.features.news.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.spectech.uikit.strings.relativeFromNow
import com.spectech.domain.model.NewsItem
import com.spectech.features.news.R
import com.spectech.platform.web.CustomTabsLauncher

/**
 * Single news card. Renders the hero (image or video-thumbnail-with-play),
 * title, expandable description, and a relative timestamp. Tapping a
 * video-bearing card opens the URL in a Chrome Custom Tab — the embedded
 * AVPlayer from iOS doesn't have a 1:1 Android counterpart that's worth
 * pulling in (ExoPlayer would add ~2MB), and external playback matches
 * how most Android apps handle short video assets.
 */
@Composable
fun NewsCard(
    item: NewsItem,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    var isExpanded by remember(item.id) { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
            Hero(item = item, onPlayVideo = { url -> CustomTabsLauncher.launch(ctx, url) })

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                val description = item.description?.takeIf { it.isNotBlank() }
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                        overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                    )
                    // Pretend-toggle: we always show the button when a description
                    // is present and >3 lines is likely. Cheaper than the iOS
                    // GeometryReader measurement and good enough — tapping a
                    // short description just re-renders identical content.
                    if (description.length > DESCRIPTION_LIKELY_TRUNCATES_AT) {
                        TextButton(
                            onClick = { isExpanded = !isExpanded },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    if (isExpanded) R.string.news_show_less
                                    else R.string.news_read_more,
                                ),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }

                Text(
                    text = item.createdAt.relativeFromNow(ctx),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Hero(
    item: NewsItem,
    onPlayVideo: (String) -> Unit,
) {
    val mediaModifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)

    val imageUrl = item.imageUrl?.takeIf { it.isNotBlank() }
    val videoUrl = item.videoUrl?.takeIf { it.isNotBlank() }

    when {
        videoUrl != null -> VideoThumbnail(
            imageUrl = imageUrl,
            onPlay = { onPlayVideo(videoUrl) },
            modifier = mediaModifier,
        )
        imageUrl != null -> AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = mediaModifier,
        )
        // No media: skip the hero entirely so text-only posts look tidy.
        else -> Unit
    }
}

@Composable
private fun VideoThumbnail(
    imageUrl: String?,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.clickable(onClick = onPlay)) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Photo,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.news_play_video),
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

// Rough heuristic: description longer than ~180 chars is almost certainly
// going to truncate at 3 lines on a phone-width card. Tuning this avoids
// the iOS GeometryReader callout, which would mean measuring text at draw
// time on every recomposition.
private const val DESCRIPTION_LIKELY_TRUNCATES_AT = 180
