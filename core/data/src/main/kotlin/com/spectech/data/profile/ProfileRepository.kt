package com.spectech.data.profile

import com.spectech.data.auth.SessionStore
import com.spectech.domain.enums.UserRole
import com.spectech.domain.model.User
import com.spectech.network.endpoints.ProfileApi
import com.spectech.network.endpoints.UpdateProfileResponse
import com.spectech.network.http.ApiClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Server-side profile edits. Mirrors iOS `EditProfileView.saveProfile`. On
 * success the canonical user record is merged back into [SessionStore] so the
 * rest of the app sees the new name/email/city immediately.
 *
 * The caller in `:features:profile` is responsible for input validation and
 * error surfacing — this repository just round-trips and persists.
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val api: ApiClient,
    private val sessionStore: SessionStore,
    private val profileStore: ProfileStore,
) {

    /**
     * Returns the updated [User] after writing it back into [SessionStore]
     * AND mirroring the freshly typed name + email into [ProfileStore]'s
     * local cache. The cache acts as a fallback for
     * [com.spectech.features.profile.ui.ProfileScreen]'s display fields
     * when the server returns a stripped User (the PATCH response is
     * intentionally lean per [mergeIntoCurrentUser]'s notes — a thin
     * response shouldn't blank the profile header).
     *
     * Mirrors iOS' overall pattern even though iOS' `EditProfileView` itself
     * only writes to `sessionStore`; on Android we proactively keep the
     * local cache in sync so the fallback chain stays meaningful.
     */
    suspend fun updateProfile(name: String, email: String, city: String): User {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedCity = city.trim()
        val response = api.send<UpdateProfileResponse>(
            ProfileApi.UpdateProfile(
                name = trimmedName,
                email = trimmedEmail,
                city = trimmedCity,
            ),
        )
        val payload = response.data
        val merged = mergeIntoCurrentUser(payload)
        sessionStore.updateUser(merged)
        // Mirror into the local cache so a stripped server payload doesn't
        // erase what the user just typed in the form. Empty input keeps the
        // existing cached value rather than wiping it — same defensive
        // semantics iOS' fallback chain relies on.
        profileStore.update {
            it.copy(
                displayName = trimmedName.ifEmpty { it.displayName },
                email = trimmedEmail.ifEmpty { it.email },
            )
        }
        return merged
    }

    /**
     * Preserves the createdAt timestamp and any other fields the backend
     * doesn't echo back (the PATCH response is intentionally lean). If there's
     * no current session this falls back to a freshly constructed user — that
     * shouldn't happen in practice since the call is auth-required.
     */
    private fun mergeIntoCurrentUser(payload: UpdateProfileResponse): User {
        val existing = sessionStore.currentUser
        return User(
            id = payload.id,
            phone = payload.phone,
            role = UserRole.fromWire(payload.role),
            name = payload.name,
            email = payload.email,
            city = payload.city,
            createdAt = existing?.createdAt,
        )
    }
}
