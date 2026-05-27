package com.spectech.platform.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads a [Bitmap] from a content URI (typically returned by the Photo
 * Picker). Decodes on [Dispatchers.IO] and applies EXIF rotation so portrait
 * photos don't end up sideways.
 *
 * Fails-soft: returns null if the URI can't be opened or decoded; the caller
 * silently drops the photo from the upload set.
 */
@Singleton
class UriBitmapLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun loadBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val decoded = try {
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        } ?: return@withContext null

        // Re-open the URI for EXIF (the previous stream is consumed).
        val rotated = try {
            resolver.openInputStream(uri)?.use { exifStream ->
                ImageEncoder.applyExifRotation(decoded, exifStream)
            }
        } catch (_: Exception) {
            null
        }
        rotated ?: decoded
    }
}
