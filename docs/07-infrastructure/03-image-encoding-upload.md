# 03 — Image Encoding & Upload

iOS source: `SpecTechIOS/Design/ImageEncoder.swift` and
`SpecTechIOS/Services/EquipmentService.swift` (`uploadPhoto`, `buildMultipartBody`).

## Image encoding requirements (match iOS)

- **Max longest edge:** 1600 px (resize down if larger)
- **Max file size:** 2,000,000 bytes (~2 MB) — keep comfortably under the
  server's 8 MB multer limit
- **Quality:** start at 0.7, decrement by 0.1 until ≤ maxBytes; final
  fallback is 0.2
- **Format:** JPEG only

## Kotlin port

```kotlin
object ImageEncoder {
    private const val MAX_BYTES = 2_000_000
    private const val MAX_EDGE = 1600

    /** Returns JPEG bytes ready for multipart upload, or null on failure. */
    fun jpegBytes(bitmap: Bitmap): ByteArray? {
        val resized = resize(bitmap, MAX_EDGE)
        for (qualityStep in 7 downTo 2) {  // 0.7 → 0.2
            val quality = qualityStep * 10
            val out = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val bytes = out.toByteArray()
            if (bytes.size <= MAX_BYTES) return bytes
        }
        // Fallback: 0.2 quality even if still over MAX_BYTES
        val out = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 20, out)
        return out.toByteArray()
    }

    private fun resize(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / longest
        val width = (bitmap.width * scale).toInt()
        val height = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
```

Run this on `Dispatchers.Default` since both resize and JPEG compression are
CPU-bound:

```kotlin
suspend fun jpegBytesAsync(bitmap: Bitmap): ByteArray? =
    withContext(Dispatchers.Default) { ImageEncoder.jpegBytes(bitmap) }
```

## Multipart upload

Backend endpoint: `POST /equipment/photos/upload`. Field name **must be**
`file`, filename `photo.jpg`, MIME `image/jpeg`. Response is
`{ url: string }` (envelope-wrapped).

### Critical HMAC interaction

The server's signature verifier hashes **the empty string** for
non-`application/json` request bodies. So our HMAC interceptor must hash an
empty body for multipart requests — already handled per
[04-networking/02-hmac-signing.md](../04-networking/02-hmac-signing.md).

### Ktor send

```kotlin
class EquipmentRepository @Inject constructor(
    private val client: HttpClient,
    private val baseUrl: String,
    private val events: AppEventBus,
) {

    suspend fun uploadPhoto(bitmap: Bitmap): String {
        val jpegBytes = ImageEncoder.jpegBytes(bitmap)
            ?: throw ApiError(message = "Could not prepare the photo for upload.")

        val response = client.post("$baseUrl/equipment/photos/upload") {
            setBody(MultiPartFormDataContent(
                parts = formData {
                    append(
                        key = "file",
                        value = jpegBytes,
                        headers = Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=\"photo.jpg\"")
                        }
                    )
                }
            ))
        }
        // Envelope: { success, data: { url } }
        return response.body<ApiEnvelope<UploadedPhotoPayload>>().data.url
    }
}

@Serializable
data class UploadedPhotoPayload(val url: String)
```

### OkHttp alternative

If you don't want Ktor's multipart helper, drop down to OkHttp:

```kotlin
val multipart = MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart(
        "file",
        "photo.jpg",
        jpegBytes.toRequestBody("image/jpeg".toMediaType()),
    )
    .build()

val request = Request.Builder()
    .url("$baseUrl/equipment/photos/upload")
    .post(multipart)
    .build()
```

Either way, the HMAC interceptor sees a body with Content-Type
`multipart/form-data; boundary=…` and signs an empty payload — matching iOS.

## Concurrent uploads

iOS uses `withThrowingTaskGroup` for parallel uploads. Kotlin port:

```kotlin
suspend fun uploadAll(bitmaps: List<Bitmap>): List<String> = coroutineScope {
    bitmaps.map { bmp -> async { equipmentRepo.uploadPhoto(bmp) } }.awaitAll()
}
```

`awaitAll` cancels in-flight uploads on first failure (same as iOS's
ThrowingTaskGroup throw-on-failure semantics).

## Photo picker → Bitmap

```kotlin
val pickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4),
) { uris ->
    coroutineScope.launch {
        val bitmaps = uris.take(4).mapNotNull { uri ->
            withContext(Dispatchers.IO) {
                ctx.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        }
        viewModel.selectedPhotos.clear()
        viewModel.selectedPhotos.addAll(bitmaps)
    }
}
```

Use `BitmapFactory.Options.inSampleSize` to downsample large images before
loading into memory if the device is low on heap — but for 4 photos at
1600 px max edge, this is usually unnecessary.

## EXIF orientation

User photos often have an EXIF rotation tag (portrait shot turns into a
landscape bitmap). Apply the rotation before encoding:

```kotlin
fun ContentResolver.bitmapWithEXIFRotation(uri: Uri): Bitmap? {
    val orientation = openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL

    val bitmap = openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
```

iOS handles EXIF rotation automatically via `UIImage`. The Android port must
do it manually or photos will appear sideways.
