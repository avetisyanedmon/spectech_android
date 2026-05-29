package com.spectech.data.notifications

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.spectech.domain.model.AppNotification
import com.spectech.domain.model.NotificationNavigationRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.notificationsDataStore by preferencesDataStore("notifications")

/**
 * In-app notifications inbox. Mirrors iOS `NotificationStore`. Persisted to
 * Preferences DataStore as a JSON-encoded list, capped at [MAX_STORED] entries.
 *
 * The store also publishes one-shot [navigationRequest] events that the root
 * navigator consumes to deep-link the user to the relevant order detail.
 */
@Singleton
class NotificationStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _navigationRequest = MutableStateFlow<NotificationNavigationRequest?>(null)
    val navigationRequest: StateFlow<NotificationNavigationRequest?> = _navigationRequest.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Live count of unread entries — drives the bell-icon badge. */
    val unreadCount: StateFlow<Int> = _notifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    init {
        scope.launch {
            val raw = context.notificationsDataStore.data.first()[KEY_STORE]
            if (!raw.isNullOrEmpty()) {
                _notifications.value = runCatching {
                    json.decodeFromString<List<AppNotification>>(raw)
                }.getOrDefault(emptyList())
            }
        }
    }

    /**
     * Prepend a fresh notification, deduping against an existing entry that
     * carries the same [AppNotification.dedupeKey]. The previous entry's
     * `isRead` is preserved so a re-pushed notification doesn't flip to
     * "unread" if the user had already seen it.
     */
    fun add(notification: AppNotification) {
        val current = _notifications.value
        val dedupeKey = notification.dedupeKey
        val next: AppNotification
        val deduped: List<AppNotification>
        if (dedupeKey != null) {
            val existing = current.firstOrNull { it.dedupeKey == dedupeKey }
            next = if (existing != null) notification.copy(isRead = existing.isRead) else notification
            deduped = current.filterNot { it.dedupeKey == dedupeKey }
        } else {
            next = notification
            deduped = current
        }
        _notifications.value = (listOf(next) + deduped).take(MAX_STORED)
        persist()
    }

    fun markRead(id: String) {
        _notifications.value = _notifications.value
            .map { if (it.id == id) it.copy(isRead = true) else it }
        persist()
    }

    fun markAllRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
        persist()
    }

    fun clear() {
        _notifications.value = emptyList()
        persist()
    }

    /** Publishes a deep-link request the root navigator should consume. */
    fun requestNavigation(request: NotificationNavigationRequest) {
        if (request.orderId.isBlank()) return
        _navigationRequest.value = request
    }

    /** Idempotent ack — called by the navigator after routing completes. */
    fun finishNavigationRequest(request: NotificationNavigationRequest) {
        if (_navigationRequest.value?.id == request.id) {
            _navigationRequest.value = null
        }
    }

    private fun persist() {
        val snapshot = _notifications.value
        scope.launch {
            context.notificationsDataStore.edit { prefs ->
                prefs[KEY_STORE] = json.encodeToString(snapshot)
            }
        }
    }

    companion object {
        const val MAX_STORED = 100
        private val KEY_STORE = stringPreferencesKey("app_notifications_v1")
        private val json = Json { ignoreUnknownKeys = true }
    }
}
