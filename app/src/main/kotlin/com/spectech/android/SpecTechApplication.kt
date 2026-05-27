package com.spectech.android

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Implements [ImageLoaderFactory] so Coil auto-discovers our caching policy on
 * first use (50 MB memory / 200 MB disk — matches iOS `SDImageCache`
 * configuration in `AppDelegate.setupSDWebImage()`).
 */
@HiltAndroidApp
class SpecTechApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        configureLogging()
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

    companion object {
        private const val MEMORY_CACHE_BYTES = 50 * 1024 * 1024
        private const val DISK_CACHE_BYTES = 200L * 1024 * 1024
    }
}
