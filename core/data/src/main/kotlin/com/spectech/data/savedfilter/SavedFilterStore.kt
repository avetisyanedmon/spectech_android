package com.spectech.data.savedfilter

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.spectech.domain.error.ApiError
import com.spectech.domain.model.OrderFilters
import com.spectech.network.endpoints.SavedFilterApi
import com.spectech.network.endpoints.SavedFilterDeletedResponse
import com.spectech.network.endpoints.SavedFilterResponse
import com.spectech.network.http.ApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

private val Context.savedFilterDataStore by preferencesDataStore("saved_filter")

/**
 * Persists the user-saved marketplace filter and their opt-in for push
 * notifications about new matching orders. Mirrors iOS `SavedFilterStore`.
 *
 * State precedence (matches iOS):
 *   - DataStore is the source of truth for the initial UI on launch (so the
 *     toggle / chip selection doesn't flicker when offline).
 *   - [loadFromServer] reconciles after sign-in. A 404 means "no row yet,
 *     keep local."
 *   - Every save / toggle / clear is mirrored to both stores; backend
 *     failures surface via [lastSyncError] rather than being silently dropped.
 *
 * Notification-permission prompting lives at the UI layer (Phase 11 already
 * wired the launcher in `MainActivity`); enable callers should ensure the
 * runtime permission is granted before flipping [setNotificationsEnabled]
 * to true.
 */
@Singleton
class SavedFilterStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiClient,
    private val json: Json,
) {

    private val _savedFilter = MutableStateFlow<OrderFilters?>(null)
    val savedFilter: StateFlow<OrderFilters?> = _savedFilter.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()

    init {
        // DataStore reads are cold flows — hydrate on construction so the
        // UI's first composition sees the persisted state, not the defaults.
        scope.launch { hydrateFromLocal() }
    }

    private suspend fun hydrateFromLocal() {
        val prefs = context.savedFilterDataStore.data.first()
        prefs[FILTER_KEY]?.let { raw ->
            _savedFilter.value = runCatching { json.decodeFromString<OrderFilters>(raw) }
                .onFailure { Timber.tag(TAG).w(it, "Local saved filter JSON malformed; ignoring") }
                .getOrNull()
        }
        _notificationsEnabled.value = prefs[ENABLED_KEY] ?: false
    }

    /** Persist the user's pick locally and push it to the backend. */
    suspend fun save(filter: OrderFilters) {
        _savedFilter.value = filter
        persistFilterLocally(filter)
        syncToServer(filter, _notificationsEnabled.value)
    }

    /**
     * Toggle the notifications opt-in. Caller is responsible for:
     *   - The runtime `POST_NOTIFICATIONS` permission flow on API 33+.
     *   - Pre-flighting the "saved filter exists" check before passing
     *     [enabled] = true. The UI surfaces a localized "save filters first"
     *     message via `R.string.filters_saved_empty_error` in that case so
     *     the message respects the user's language pick — keeping it out of
     *     this layer means the data module stays free of resource lookups.
     *
     * When enabled without a saved filter we simply no-op the server push;
     * the local toggle flips but nothing is registered with the matcher.
     * Once the user saves filters, [save] will push the upsert with the
     * current `notificationsEnabled` value, picking up where they left off.
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        persistEnabledLocally(enabled)

        if (enabled) {
            val filter = _savedFilter.value ?: return
            syncToServer(filter, true)
        } else {
            disableOnServer()
        }
    }

    /** Wipe local filter + clear on the server. Called from sign-out / UI clear. */
    suspend fun clear() {
        _savedFilter.value = null
        context.savedFilterDataStore.edit { it.remove(FILTER_KEY) }
        deleteOnServer()
    }

    /** Best-effort local-only wipe. Used at sign-out before the auth token is dropped. */
    suspend fun forgetLocal() {
        _savedFilter.value = null
        _notificationsEnabled.value = false
        _lastSyncError.value = null
        runCatching {
            context.savedFilterDataStore.edit { prefs ->
                prefs.remove(FILTER_KEY)
                prefs.remove(ENABLED_KEY)
            }
        }
    }

    /** Reconcile local state with the server. 404 → no row yet; keep local. */
    suspend fun loadFromServer() {
        syncMutex.withLock {
            _isSyncing.value = true
            try {
                val envelope = api.send<SavedFilterResponse>(SavedFilterApi.Fetch)
                val response = envelope.data
                response.filters?.let { payload ->
                    val merged = payload.toOrderFilters()
                    _savedFilter.value = merged
                    persistFilterLocally(merged)
                }
                response.enabled?.let { enabled ->
                    _notificationsEnabled.value = enabled
                    persistEnabledLocally(enabled)
                }
                _lastSyncError.value = null
            } catch (e: ApiError) {
                if (e.statusCode == 404) {
                    _lastSyncError.value = null
                } else {
                    _lastSyncError.value = e.message
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "loadFromServer failed")
                _lastSyncError.value = e.localizedMessage ?: e.message
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun syncToServer(filter: OrderFilters, enabled: Boolean) {
        syncMutex.withLock {
            _isSyncing.value = true
            try {
                api.send<SavedFilterResponse>(SavedFilterApi.Upsert(filter.toPayload(), enabled))
                _lastSyncError.value = null
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "upsert failed")
                _lastSyncError.value = e.localizedMessage ?: e.message
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun disableOnServer() {
        syncMutex.withLock {
            _isSyncing.value = true
            try {
                api.send<SavedFilterResponse>(SavedFilterApi.SetEnabled(false))
                _lastSyncError.value = null
            } catch (e: ApiError) {
                if (e.statusCode == 404) _lastSyncError.value = null
                else _lastSyncError.value = e.message
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "setEnabled(false) failed")
                _lastSyncError.value = e.localizedMessage ?: e.message
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun deleteOnServer() {
        syncMutex.withLock {
            _isSyncing.value = true
            try {
                api.send<SavedFilterDeletedResponse>(SavedFilterApi.Delete)
                _lastSyncError.value = null
            } catch (e: ApiError) {
                if (e.statusCode == 404) _lastSyncError.value = null
                else _lastSyncError.value = e.message
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "delete failed")
                _lastSyncError.value = e.localizedMessage ?: e.message
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun persistFilterLocally(filter: OrderFilters) {
        runCatching {
            context.savedFilterDataStore.edit { prefs ->
                prefs[FILTER_KEY] = json.encodeToString(filter)
            }
        }.onFailure { Timber.tag(TAG).w(it, "Failed to persist filter locally") }
    }

    private suspend fun persistEnabledLocally(enabled: Boolean) {
        runCatching {
            context.savedFilterDataStore.edit { prefs -> prefs[ENABLED_KEY] = enabled }
        }.onFailure { Timber.tag(TAG).w(it, "Failed to persist enabled flag locally") }
    }

    companion object {
        private const val TAG = "SavedFilterStore"
        private val FILTER_KEY = stringPreferencesKey("filter_v1")
        private val ENABLED_KEY = booleanPreferencesKey("notifications_enabled_v1")
    }
}
