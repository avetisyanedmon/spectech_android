package com.spectech.platform.image

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Encodes a [Bitmap] to JPEG bytes ready for multipart upload. Mirrors iOS
 * `ImageEncoder.jpegData(_:)` in SpecTechIOS/Design/ImageEncoder.swift —
 * longest edge capped at 1600 px, JPEG quality stepped down from 0.7 to 0.2
 * until the encoded blob fits under ~2 MB.
 */
object ImageEncoder {
    private const val MAX_BYTES = 2_000_000
    private const val MAX_EDGE = 1600

    /** Resize + compress. Returns null only if even quality 0.2 isn't reachable. */
    fun jpegBytes(bitmap: Bitmap): ByteArray {
        val resized = resize(bitmap, MAX_EDGE)
        for (qualityStep in 7 downTo 2) {
            val quality = qualityStep * 10
            val out = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val bytes = out.toByteArray()
            if (bytes.size <= MAX_BYTES) return bytes
        }
        // Final fallback: lowest acceptable quality, return whatever we have.
        return ByteArrayOutputStream().apply {
            resized.compress(Bitmap.CompressFormat.JPEG, 20, this)
        }.toByteArray()
    }

    private fun resize(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / longest
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    /**
     * Reads the EXIF orientation flag from the source stream and applies the
     * corresponding rotation to [bitmap]. Users who take photos in portrait
     * end up with rotated bitmaps that need this fix-up; iOS gets it for free
     * via UIImage.
     */
    fun applyExifRotation(bitmap: Bitmap, exifSource: InputStream): Bitmap {
        val orientation = try {
            ExifInterface(exifSource).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
