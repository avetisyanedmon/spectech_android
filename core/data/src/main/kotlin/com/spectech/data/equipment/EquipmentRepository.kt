package com.spectech.data.equipment

import com.spectech.data.events.AppEventBus
import com.spectech.data.events.DomainEvent
import com.spectech.domain.model.CreateEquipmentRequest
import com.spectech.domain.model.Equipment
import com.spectech.domain.model.UpdateEquipmentRequest
import com.spectech.network.endpoints.EquipmentApi
import com.spectech.network.http.ApiClient
import com.spectech.network.http.ApiEnvelope
import com.spectech.platform.config.AppConfiguration
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable

/**
 * Wraps the `/equipment` endpoint family. CRUD goes through [ApiClient] for
 * consistent envelope handling; the photo upload uses [httpClient] directly
 * because it's the only call that sends `multipart/form-data` — the HMAC
 * interceptor in [HmacInterceptor] hashes an empty body for non-JSON content,
 * matching the iOS contract.
 *
 * All mutations emit [DomainEvent.EquipmentChanged] so the Garage tab and any
 * open bid sheet refresh automatically. Mirrors iOS `EquipmentService`.
 */
@Singleton
class EquipmentRepository @Inject constructor(
    private val api: ApiClient,
    private val httpClient: HttpClient,
    private val config: AppConfiguration,
    private val events: AppEventBus,
) {

    /** Whole garage list for the current contractor. */
    suspend fun fetchEquipment(): List<Equipment> =
        api.send<List<Equipment>>(EquipmentApi.FetchAll).data

    suspend fun createEquipment(request: CreateEquipmentRequest): Equipment {
        val response = api.send<Equipment>(EquipmentApi.Create(request)).data
        events.emit(DomainEvent.EquipmentChanged)
        return response
    }

    suspend fun updateEquipment(id: String, request: UpdateEquipmentRequest): Equipment {
        val response = api.send<Equipment>(EquipmentApi.Update(id, request)).data
        events.emit(DomainEvent.EquipmentChanged)
        return response
    }

    suspend fun deleteEquipment(id: String) {
        api.send<DeletedEquipmentId>(EquipmentApi.Delete(id))
        events.emit(DomainEvent.EquipmentChanged)
    }

    /**
     * Uploads a single JPEG via `multipart/form-data` to
     * `POST /equipment/photos/upload`. Returns the hosted URL the backend
     * persists in the equipment's `photos` array.
     */
    suspend fun uploadPhoto(jpegBytes: ByteArray): String {
        val url = "${config.apiBaseUrl.trimEnd('/')}/equipment/photos/upload"
        val response = httpClient.post(url) {
            setBody(MultiPartFormDataContent(
                formData {
                    append(
                        key = "file",
                        value = jpegBytes,
                        headers = Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, """filename="photo.jpg"""")
                        },
                    )
                }
            ))
        }
        val envelope: ApiEnvelope<UploadedPhotoPayload> = response.body()
        return envelope.data.url
    }
}

@Serializable
private data class UploadedPhotoPayload(val url: String)

@Serializable
private data class DeletedEquipmentId(val id: String? = null)
