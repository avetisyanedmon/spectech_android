package com.spectech.features.profile.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.spectech.domain.enums.AppLanguage

/**
 * Thin wrapper around `AppCompatDelegate.setApplicationLocales`. Kept in its
 * own file so call sites don't drag the AppCompat import everywhere, and so a
 * future move to a different locale strategy (per-locale resource overlays,
 * Compose `LocaleConfiguration`) has a single seam to flip.
 *
 * Compose recomposes the configuration automatically after the call, so views
 * pick up the new strings on the next frame without restarting the activity.
 */
object LocaleManager {

    /** Applies [language] system-wide. Survives process death (per-app locale). */
    fun apply(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.locale),
        )
    }

    /** Reads the currently-applied per-app locale, if any. */
    fun currentLanguage(): AppLanguage? {
        val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (tag.isEmpty()) return null
        return AppLanguage.fromLocaleTag(tag)
    }
}
