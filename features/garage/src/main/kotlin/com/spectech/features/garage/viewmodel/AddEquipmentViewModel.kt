package com.spectech.features.garage.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectech.data.equipment.EquipmentRepository
import com.spectech.domain.enums.EquipmentCategory
import com.spectech.domain.error.ApiError
import com.spectech.domain.model.CreateEquipmentRequest
import com.spectech.platform.image.ImageEncoder
import com.spectech.platform.image.UriBitmapLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Add Equipment form. Mirrors iOS `AddEquipmentViewModel`.
 *
 * Uploads selected photos concurrently (matches iOS `withThrowingTaskGroup`)
 * before submitting the create request; if any upload fails, the create is
 * abandoned and the user keeps their form state.
 */
@HiltViewModel
class AddEquipmentViewModel @Inject constructor(
    private val equipmentRepo: EquipmentRepository,
    private val bitmapLoader: UriBitmapLoader,
) : ViewModel() {

    // Form state ----------------------------------------------------------------
    var name by mutableStateOf("")
    var category by mutableStateOf<EquipmentCategory?>(null)
    var vin by mutableStateOf("")
    var yearOfManufacture by mutableStateOf("")
    var description by mutableStateOf("")
    var additionalEquipment by mutableStateOf("")

    val selectedPhotos: SnapshotStateList<Uri> = emptyList<Uri>().toMutableStateList()

    // Submission state ---------------------------------------------------------
    var isSubmitting by mutableStateOf(false)
        private set
    var error by mutableStateOf<ApiError?>(null)
    var success by mutableStateOf(false)
        private set

    val canSubmit: Boolean
        get() = category != null &&
                name.isNotBlank() &&
                vin.isNotBlank() &&
                yearOfManufacture.isNotBlank() &&
                !isSubmitting

    fun addPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val remaining = (MAX_PHOTOS - selectedPhotos.size).coerceAtLeast(0)
        selectedPhotos.addAll(uris.take(remaining))
    }

    fun removePhoto(uri: Uri) {
        selectedPhotos.remove(uri)
    }

    fun submit() {
        val chosenCategory = category ?: run {
            error = ApiError(
                code = ApiError.LocalCodes.GARAGE_CATEGORY_REQUIRED,
                message = "Select an equipment type.",
            )
            return
        }
        if (isSubmitting) return

        viewModelScope.launch {
            isSubmitting = true
            error = null
            try {
                // Upload all photos concurrently — fails fast if any one errors.
                val photoUrls = uploadPhotos()
                val characteristics = composeCharacteristics()
                equipmentRepo.createEquipment(
                    CreateEquipmentRequest(
                        name = name.trim(),
                        category = chosenCategory,
                        characteristics = characteristics,
                        additionalEquipment = additionalEquipment.trim(),
                        photos = photoUrls,
                    )
                )
                success = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiError) {
                error = e
            } catch (e: Exception) {
                error = ApiError.from(e)
            } finally {
                isSubmitting = false
            }
        }
    }

    /**
     * Wipe every field the user has touched. Called from [AddEquipmentSheet]
     * on each sheet open so the form starts blank — Hilt scopes this VM to
     * the surrounding NavBackStackEntry, so without an explicit reset the
     * stale name/category/photo URIs from a previous open leak in. The
     * stale photo URIs are the real hazard: the Photo Picker's temporary
     * read grant has expired by the time the user submits, so re-uploading
     * them surfaces a misleading "Could not read one of the photos" error.
     */
    fun clearForm() {
        name = ""
        category = null
        vin = ""
        yearOfManufacture = ""
        description = ""
        additionalEquipment = ""
        selectedPhotos.clear()
        error = null
        success = false
    }

    /**
     * Flip [success] back to false after the sheet has consumed the signal
     * to dismiss. Without this, the LaunchedEffect that observes [success]
     * would fire again the next time the sheet recomposes and immediately
     * close it.
     */
    fun consumeSuccess() {
        success = false
    }

    // Helpers ------------------------------------------------------------------

    private suspend fun uploadPhotos(): List<String> = coroutineScope {
        selectedPhotos.toList().map { uri ->
            async {
                val bitmap = bitmapLoader.loadBitmap(uri)
                    ?: throw ApiError(
                        code = ApiError.LocalCodes.GARAGE_PHOTO_READ_FAILED,
                        message = "Could not read one of the photos.",
                    )
                val bytes = ImageEncoder.jpegBytes(bitmap)
                equipmentRepo.uploadPhoto(bytes)
            }
        }.awaitAll()
    }

    private fun composeCharacteristics(): String {
        val parts = buildList {
            add("VIN: ${vin.trim()}")
            add("Year: ${yearOfManufacture.trim()}")
            description.trim().takeIf { it.isNotEmpty() }?.let { add("Description: $it") }
        }
        return parts.joinToString(" | ")
    }

    companion object {
        const val MAX_PHOTOS = 4
    }
}
