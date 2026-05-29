package com.spectech.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * UI language the user explicitly picked in Profile. Mirrors iOS `AppLanguage`
 * (SpecTechIOS/Shared/Models/DomainModels.swift).
 *
 * [locale] is the BCP-47 tag fed to `AppCompatDelegate.setApplicationLocales`.
 * [wire] matches the iOS Codable raw value so an account that signs in across
 * both platforms keeps the same picked language.
 */
@Serializable
enum class AppLanguage(val wire: String, val locale: String) {
    @SerialName("english") ENGLISH("english", "en"),
    @SerialName("russian") RUSSIAN("russian", "ru"),
    ;

    companion object {
        /** Resolves a BCP-47 tag to a known language, defaulting to English. */
        fun fromLocaleTag(tag: String?): AppLanguage {
            val short = tag?.substringBefore('-')?.lowercase()
            return entries.firstOrNull { it.locale == short } ?: ENGLISH
        }
    }
}
