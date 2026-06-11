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
import com.spectech.domain.error.ApiError
import com.spectech.domain.model.Equipment
import com.spectech.domain.model.UpdateEquipmentRequest
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
 * Edit Equipment form. iOS keeps the original VIN/Year locked once an
 * equipment unit exists (the backend doesn't accept changes to them), so we
 * surface name / description / additional equipment / photos only.
 *
 * Existing photo URLs and freshly picked ones are uploaded together — new
 * ones go through the same upload pipeline as Add Equipment.
 */
@HiltViewModel
class EditEquipmentViewModel @Inject constructor(
    private val equipmentRepo: EquipmentRepository,
    private val bitmapLoader: UriBitmapLoader,
) : ViewModel() {

    private var equipmentId: String? = null

    var name by mutableStateOf("")
    var description by mutableStateOf("")
    var additionalEquipment by mutableStateOf("")

    /** Photo URLs already on the server. Renamed/removed in place. */
    val existingPhotoUrls: SnapshotStateList<String> = emptyList<String>().toMutableStateList()

    /** Fresh photos to upload on submit. */
    val pendingPhotos: SnapshotStateList<Uri> = emptyList<Uri>().toMutableStateList()

    var isSubmitting by mutableStateOf(false)
        private set
    var error by mutableStateOf<ApiError?>(null)
    var success by mutableStateOf(false)
        private set

    val canSubmit: Boolean get() = name.isNotBlank() && !isSubmitting

    val totalPhotoCount: Int get() = existingPhotoUrls.size + pendingPhotos.size

    /**
     * Always re-seed from the latest equipment snapshot. The previous early-
     * return guard ("if (equipmentId == equipment.id) return") made the form
     * show stale photo URLs after a successful submit + remote refresh, so it
     * was dropped — [EditEquipmentSheet] calls [clearForm] first, then this,
     * on every open.
     */
    fun seedFrom(equipment: Equipment) {
        equipmentId = equipment.id
        name = equipment.name
        description = extractDescription(equipment.characteristics)
        additionalEquipment = equipment.additionalEquipment.orEmpty()
        existingPhotoUrls.clear()
        existingPhotoUrls.addAll(equipment.photos)
    }

    fun addPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val remaining = (MAX_PHOTOS - totalPhotoCount).coerceAtLeast(0)
        pendingPhotos.addAll(uris.take(remaining))
    }

    fun removeExistingPhoto(url: String) {
        existingPhotoUrls.remove(url)
    }

    fun removePendingPhoto(uri: Uri) {
        pendingPhotos.remove(uri)
    }

    fun submit() {
        val id = equipmentId ?: return
        if (isSubmitting) return

        viewModelScope.launch {
            isSubmitting = true
            error = null
            try {
                val newlyUploaded = uploadPending()
                val mergedPhotos = existingPhotoUrls.toList() + newlyUploaded
                equipmentRepo.updateEquipment(
                    id = id,
                    request = UpdateEquipmentRequest(
                        name = name.trim(),
                        additionalEquipment = additionalEquipment.trim(),
                        photos = mergedPhotos,
                    ),
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
     * Wipe pending photos and submission state so the sheet starts from a
     * clean slate. Called from [EditEquipmentSheet] on each open. The VM is
     * Hilt-scoped to the surrounding NavBackStackEntry, so without an
     * explicit reset, leftover `pendingPhotos` URIs from a previous open
     * resurface — and their Photo Picker read grant has expired, which the
     * upload reports as "Could not read one of the photos".
     *
     * Doesn't touch `name`/`description`/`additionalEquipment`/`existingPhotoUrls`
     * because [seedFrom] overwrites them immediately afterwards.
     */
    fun clearForm() {
        pendingPhotos.clear()
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

    private suspend fun uploadPending(): List<String> {
        if (pendingPhotos.isEmpty()) return emptyList()
        return coroutineScope {
            pendingPhotos.toList().map { uri ->
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
    }

    /** Pulls the user's free-form description out of the pipe-delimited blob. */
    private fun extractDescription(characteristics: String): String =
        characteristics.split("|").map { it.trim() }
            .firstOrNull { it.startsWith("Description:") }
            ?.removePrefix("Description:")
            ?.trim()
            .orEmpty()

    companion object {
        const val MAX_PHOTOS = 4
    }
}
