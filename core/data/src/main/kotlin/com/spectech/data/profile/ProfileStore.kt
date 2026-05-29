package com.spectech.data.profile

import com.spectech.domain.model.LocalProfile
import com.spectech.platform.storage.SecureStorage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Local-only user preferences (language pick, notifications toggle, cached
 * profile fields). Mirrors iOS `ProfileStore`
 * (SpecTechIOS/Features/Profile/ProfileStore.swift). Persisted as JSON in
 * [SecureStorage] under [KEY_LOCAL_PROFILE].
 *
 * The store hydrates synchronously on first access — `init` reads from secure
 * storage so the very first composition can read a non-default value when the
 * user has set their language on a previous launch. Writes happen under a
 * mutex so the in-memory copy and the persisted JSON never diverge.
 */
@Singleton
class ProfileStore @Inject constructor(
    private val secure: SecureStorage,
    private val json: Json,
) {

    private val _profile = MutableStateFlow(loadFromSecureStorage())
    val profile: StateFlow<LocalProfile> = _profile.asStateFlow()

    private val writeMutex = Mutex()

    val current: LocalProfile get() = _profile.value

    /**
     * Atomically read–transform–write the local profile. The transform runs
     * inside the mutex so concurrent edits (e.g. language picker + edit form
     * landing in the same frame) can't drop a change.
     */
    suspend fun update(transform: (LocalProfile) -> LocalProfile) {
        writeMutex.withLock {
            val next = transform(_profile.value)
            if (next == _profile.value) return
            _profile.value = next
            persist(next)
        }
    }

    /** Wipes the local profile — call from logout. */
    suspend fun clear() {
        writeMutex.withLock {
            _profile.value = LocalProfile()
            runCatching { secure.delete(KEY_LOCAL_PROFILE) }
                .onFailure { Timber.tag(TAG).w(it, "Failed to clear local profile") }
        }
    }

    private fun loadFromSecureStorage(): LocalProfile {
        val raw = runCatching { secure.read(KEY_LOCAL_PROFILE) }
            .onFailure { Timber.tag(TAG).w(it, "Failed to read local profile") }
            .getOrNull() ?: return LocalProfile()
        return runCatching { json.decodeFromString<LocalProfile>(raw) }
            .onFailure { Timber.tag(TAG).w(it, "Local profile JSON malformed; resetting") }
            .getOrDefault(LocalProfile())
    }

    private fun persist(profile: LocalProfile) {
        runCatching { secure.save(KEY_LOCAL_PROFILE, json.encodeToString(profile)) }
            .onFailure { Timber.tag(TAG).w(it, "Failed to persist local profile") }
    }

    companion object {
        const val KEY_LOCAL_PROFILE = "local_profile"
        private const val TAG = "ProfileStore"
    }
}
