package com.spectech.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.spectech.android.push.PushTokenSource
import com.spectech.data.auth.SessionStore
import com.spectech.data.profile.ProfileStore
import com.spectech.data.push.PushRepository
import com.spectech.data.savedfilter.SavedFilterStore
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * App entry. Owns five pieces of cross-cutting setup:
 *   1. Coil image cache config (50 MB memory / 200 MB disk — matches iOS).
 *   2. Notification channel registration (API 26+).
 *   3. Session-driven push registration — collects [SessionStore.currentSession]
 *      and calls `register`/`unregister` on the backend when the active user
 *      changes. Mirrors the iOS `onChange(of: sessionStore.currentSession)`
 *      handler in `SpecTechIOSApp`.
 *   4. Per-app locale — re-applies the saved [com.spectech.domain.model.LocalProfile.language]
 *      so the user's pick from Profile survives a fresh process start. Compose
 *      recomposes against the new configuration without an activity restart.
 *   5. Saved-filter reconcile on sign-in — pulls the user's persisted
 *      marketplace filter + notification opt-in from the server so the UI
 *      reflects what the matcher will actually subscribe against.
 */
@HiltAndroidApp
class SpecTechApplication : Application(), ImageLoaderFactory {

    @Inject lateinit var sessionStore: SessionStore
    @Inject lateinit var pushRepository: PushRepository
    @Inject lateinit var profileStore: ProfileStore
    @Inject lateinit var savedFilterStore: SavedFilterStore

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        configureLogging()
        configureNotificationChannel()
        applySavedLanguage()
        wireSessionPushRegistration()
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizeBytes(MEMORY_CACHE_BYTES)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(DISK_CACHE_BYTES)
                .build()
        }
        .crossfade(true)
        .build()

    private fun configureLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun configureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_default),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.notification_channel_default_desc)
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Applies the user's saved language pick if any. We don't override an
     * already-set per-app locale (the user may have set it via system
     * settings) — only push the saved pick when no override exists.
     */
    private fun applySavedLanguage() {
        if (!AppCompatDelegate.getApplicationLocales().isEmpty) return
        val saved = profileStore.current.language
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(saved.locale))
    }

    private fun wireSessionPushRegistration() {
        applicationScope.launch {
            sessionStore.currentSession
                .map { it?.user?.id }
                .distinctUntilChanged()
                .collectLatest { userId ->
                    if (userId == null) {
                        // Sign-out — clear in-memory registration token. We don't
                        // proactively call the backend's unregister endpoint here
                        // because we no longer have an auth token to authorize it.
                        pushRepository.forget()
                    } else {
                        val token = PushTokenSource.current()
                        if (token != null) pushRepository.registerToken(token)
                        // Reconcile the saved marketplace filter with the server.
                        // A 404 means "no row yet, keep local"; the store handles
                        // this internally so we don't need to branch here.
                        savedFilterStore.loadFromServer()
                    }
                }
        }
    }

    companion object {
        private const val MEMORY_CACHE_BYTES = 50 * 1024 * 1024
        private const val DISK_CACHE_BYTES = 200L * 1024 * 1024

        /** Single channel for all in-app push notifications. */
        const val NOTIFICATION_CHANNEL_ID = "spectech_default"
    }
}
