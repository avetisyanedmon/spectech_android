package com.spectech.data.push

import com.spectech.network.endpoints.PushApi
import com.spectech.network.endpoints.RegisterTokenResponse
import com.spectech.network.endpoints.UnregisterTokenResponse
import com.spectech.network.http.ApiClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Sends FCM device tokens to the backend. Mirrors iOS
 * `PushRegistrationService` minus the token-discovery machinery — on Android
 * the caller in `:app` is responsible for resolving the token (Firebase is
 * platform code; the data layer stays Firebase-free).
 *
 * In-memory `lastRegisteredToken` prevents spam when the lifecycle observer
 * triggers register() multiple times with the same token.
 */
@Singleton
class PushRepository @Inject constructor(
    private val api: ApiClient,
) {
    private var lastRegisteredToken: String? = null
    private val mutex = Mutex()

    suspend fun registerToken(token: String) {
        if (token.isBlank()) return
        mutex.withLock {
            if (token == lastRegisteredToken) return
            try {
                api.send<RegisterTokenResponse>(PushApi.RegisterToken(token))
                lastRegisteredToken = token
                Timber.tag(TAG).d("Registered FCM token with backend")
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to register FCM token")
            }
        }
    }

    suspend fun unregisterToken(token: String) {
        if (token.isBlank()) return
        mutex.withLock {
            try {
                api.send<UnregisterTokenResponse>(PushApi.UnregisterToken(token))
                if (lastRegisteredToken == token) lastRegisteredToken = null
                Timber.tag(TAG).d("Unregistered FCM token")
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to unregister FCM token")
            }
        }
    }

    /**
     * Best-effort server-side unregister of the last token this process
     * registered. Must run BEFORE the session is cleared — the endpoint needs
     * the bearer token — so the backend stops routing this account's pushes
     * to the device after sign-out. Failures are logged and swallowed inside
     * [unregisterToken]; logout must never be blocked by a network error.
     */
    suspend fun unregisterLastToken() {
        val token = lastRegisteredToken ?: return
        unregisterToken(token)
    }

    /** Forgets the in-memory register state, so the next sign-in re-registers. */
    fun forget() {
        lastRegisteredToken = null
    }

    companion object {
        private const val TAG = "PushRepository"
    }
}
