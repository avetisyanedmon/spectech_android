package com.spectech.domain.model

import com.spectech.domain.enums.UserRole
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Mirror of iOS `User` (SpecTechIOS/Shared/Models/DomainModels.swift).
 *
 * `id` is stored as a String to avoid pulling kotlinx-serialization Uuid (still
 * experimental on 1.7.x) into the domain module; callers convert when they need
 * `java.util.UUID` semantics. This keeps the wire format and storage shape
 * identical to iOS.
 */
@Serializable
data class User(
    val id: String,
    val phone: String,
    val role: UserRole,
    val name: String? = null,
    val email: String? = null,
    val city: String? = null,
    val createdAt: Instant? = null,
)
