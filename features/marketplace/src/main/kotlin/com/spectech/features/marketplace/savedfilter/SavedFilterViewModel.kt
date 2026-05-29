package com.spectech.features.marketplace.savedfilter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectech.data.savedfilter.SavedFilterStore
import com.spectech.domain.model.OrderFilters
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Backs the saved-filter section inside [MarketplaceFilterSheet] and the
 * "Notify me about matching orders" row in Profile. The store itself is a
 * `@Singleton` so the same source of truth backs both surfaces and stays in
 * sync as the user toggles from either.
 */
@HiltViewModel
class SavedFilterViewModel @Inject constructor(
    private val store: SavedFilterStore,
) : ViewModel() {

    val savedFilter = store.savedFilter
    val notificationsEnabled = store.notificationsEnabled
    val isSyncing = store.isSyncing
    val lastSyncError = store.lastSyncError

    fun save(filter: OrderFilters) {
        viewModelScope.launch { store.save(filter) }
    }

    /**
     * Caller must have already obtained `POST_NOTIFICATIONS` runtime
     * permission when [enabled] is true — see `permissionGranted` callback
     * in the host composable. Disabling never needs permission.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { store.setNotificationsEnabled(enabled) }
    }

    fun clear() {
        viewModelScope.launch { store.clear() }
    }
}
