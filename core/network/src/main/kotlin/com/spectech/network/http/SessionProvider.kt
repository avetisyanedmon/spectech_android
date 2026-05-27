package com.spectech.network.http

/**
 * Seam between the network layer and the session store. The ApiClient calls
 * [authToken] before every request to attach `Authorization: Bearer …` and
 * [clearSession] when the backend returns 401 so the UI can re-prompt sign-in.
 *
 * The concrete implementation lives in core/data (see `SessionStore`); Hilt
 * binds it to this interface.
 */
interface SessionProvider {
    suspend fun authToken(): String?
    suspend fun clearSession()
}
