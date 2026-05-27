package com.spectech.data.auth

import com.spectech.domain.enums.UserRole
import com.spectech.domain.model.AuthSession
import com.spectech.domain.model.User
import com.spectech.network.http.SessionProvider
import com.spectech.platform.storage.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory auth session with secure persistence. Mirrors iOS `SessionStore`
 * (SpecTechIOS/App/SessionStore.swift).
 *
 * Also implements [SessionProvider] so the network module can pull the bearer
 * token and clear the session on a 401 — Hilt binds this concrete class to the
 * interface (see BindingsModule).
 */
@Singleton
class SessionStore @Inject constructor(
    private val secure: SecureStorage,
    private val json: Json,
) : SessionProvider {

    private val _currentSession = MutableStateFlow<AuthSession?>(null)
    val currentSession: StateFlow<AuthSession?> = _currentSession.asStateFlow()

    // Starts true so the splash screen waits for the initial restore() call to
    // complete. Set to false in the finally block of restore().
    private val _isRestoring = MutableStateFlow(true)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val writeMutex = Mutex()

    val isAuthenticated: Boolean get() = _currentSession.value != null
    val currentUser: User? get() = _currentSession.value?.user

    // SessionProvider ---------------------------------------------------------

    override suspend fun authToken(): String? = _currentSession.value?.token

    override suspend fun clearSession() {
        writeMutex.withLock {
            _currentSession.value = null
            secure.delete(KEY_SESSION)
        }
    }

    // Lifecycle ---------------------------------------------------------------

    /** Called once on app launch. Reads the persisted session, if any. */
    suspend fun restore() {
        _isRestoring.value = true
        try {
            val raw = secure.read(KEY_SESSION) ?: return
            _currentSession.value = runCatching { json.decodeFromString<AuthSession>(raw) }
                .onFailure { Timber.w(it, "Stored session failed to decode; clearing") }
                .getOrNull()
            if (_currentSession.value == null) {
                secure.delete(KEY_SESSION)
            }
        } finally {
            _isRestoring.value = false
        }
    }

    /** Stores the session in memory and persists to secure storage. */
    suspend fun save(session: AuthSession) {
        writeMutex.withLock {
            _currentSession.value = session
            runCatching { secure.save(KEY_SESSION, json.encodeToString(session)) }
                .onFailure { Timber.w(it, "Failed to persist session to secure storage") }
        }
    }

    /** Updates only the embedded user (used by profile edits). */
    suspend fun updateUser(user: User) {
        val current = _currentSession.value ?: return
        save(current.copy(user = user))
    }

    /** Debug-only fast path that matches `applyTemporaryBypassSession` on iOS. */
    suspend fun applyBypassSession(phone: String) {
        save(
            AuthSession(
                token = "dev-bypass-token",
                user = User(
                    id = "00000000-0000-0000-0000-000000000001",
                    phone = phone,
                    role = UserRole.CUSTOMER,
                    createdAt = Clock.System.now(),
                ),
                isNewUser = false,
            )
        )
    }

    companion object {
        const val KEY_SESSION = "auth_session"
    }
}
