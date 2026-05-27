package com.spectech.domain.model

import com.spectech.domain.enums.EquipmentCategory
import kotlinx.serialization.Serializable

/**
 * `POST /equipment` body. Category is sent via its snake_case wire value; the
 * server normalises to the canonical form.
 */
@Serializable
data class CreateEquipmentRequest(
    val name: String,
    val category: EquipmentCategory,
    val characteristics: String,
    val additionalEquipment: String,
    val photos: List<String>,
)

/**
 * `PATCH /equipment/{id}` body. All fields optional: only the populated ones
 * are updated. Mirrors iOS `UpdateEquipmentRequest`.
 */
@Serializable
data class UpdateEquipmentRequest(
    val name: String? = null,
    val characteristics: String? = null,
    val additionalEquipment: String? = null,
    val photos: List<String>? = null,
)
