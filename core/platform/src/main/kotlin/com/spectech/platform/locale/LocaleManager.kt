package com.spectech.platform.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.spectech.domain.enums.AppLanguage

/**
 * Per-app language switching. Persists across launches (AppCompatDelegate
 * stores the choice in the framework's per-app locale storage on API 33+ and
 * a backport file on lower API levels), so the caller (Profile) does NOT also
 * need to write it to disk.
 *
 * iOS analog: SwiftUI's `String(localized:)` follows the system locale; the
 * Profile screen picker writes the choice to `LocalProfile.language` and the
 * UI re-renders. On Android the corresponding hook is this function.
 */
object LocaleManager {

    fun setLanguage(language: AppLanguage) {
        val locales = LocaleListCompat.forLanguageTags(language.locale)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun currentLanguage(): AppLanguage {
        val tag = AppCompatDelegate.getApplicationLocales()
            .toLanguageTags()
            .substringBefore(',')
            .substringBefore('-')
            .ifEmpty { return defaultFromSystemLocale() }
        return AppLanguage.entries.firstOrNull { it.locale == tag } ?: defaultFromSystemLocale()
    }

    private fun defaultFromSystemLocale(): AppLanguage {
        val systemTag = java.util.Locale.getDefault().language
        return AppLanguage.entries.firstOrNull { it.locale == systemTag } ?: AppLanguage.ENGLISH
    }
}
