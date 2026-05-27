package com.spectech.platform.web

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

/**
 * Thin wrapper around [CustomTabsIntent] for opening external web flows
 * (YooKassa deposit pages, Privacy / Terms URLs from Profile). On Android,
 * Chrome Custom Tabs is the closest counterpart to iOS `SFSafariViewController`
 * — the user stays in the app process, returns via the back/close button, and
 * preserves any cookies for follow-on flows.
 *
 * The caller observes the activity lifecycle to detect the user returning
 * (e.g. lifecycle `ON_RESUME`) and triggers a status `sync()` then.
 */
object CustomTabsLauncher {
    fun launch(context: Context, url: String) {
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(false)
            .build()
        intent.launchUrl(context, url.toUri())
    }
}
