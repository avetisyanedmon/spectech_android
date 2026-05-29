package com.spectech.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Read-only news feed entry. Mirrors iOS `NewsItem`
 * (SpecTechIOS/Scene/Tabs/News/Model/NewsItem.swift). The `/news` endpoint
 * is public so this model lands in :core:domain rather than gating behind
 * an authenticated repository.
 *
 * `videoUrl` and `imageUrl` are mutually exclusive in practice — the iOS
 * card prefers video; we mirror that hierarchy in the Android card.
 */
@Serializable
data class NewsItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val createdAt: Instant,
)
