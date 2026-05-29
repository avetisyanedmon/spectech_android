package com.spectech.domain.model

import com.spectech.domain.enums.AppLanguage
import kotlinx.serialization.Serializable

/**
 * Local-only user preferences. Mirrors iOS `LocalProfile`
 * (SpecTechIOS/Shared/Models/DomainModels.swift). Persisted in SecureStorage
 * (Keychain analog) by `ProfileStore` — kept off the backend on purpose: these
 * are device-local hints (preferred language, in-app notifications toggle,
 * cached fallbacks for fields the server might not have populated yet).
 */
@Serializable
data class LocalProfile(
    val displayName: String = "",
    val companyName: String = "",
    val email: String = "",
    val notificationsEnabled: Boolean = true,
    val language: AppLanguage = AppLanguage.ENGLISH,
)
