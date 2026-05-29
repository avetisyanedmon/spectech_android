package com.spectech.features.garage.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectech.data.auth.SessionStore
import com.spectech.data.equipment.EquipmentRepository
import com.spectech.data.events.AppEventBus
import com.spectech.data.events.DomainEvent
import com.spectech.domain.error.ApiError
import com.spectech.domain.model.Equipment
import com.spectech.domain.state.RemoteState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/**
 * Contractor's equipment inventory. Mirrors iOS `GarageListViewModel`.
 * Refreshes on [DomainEvent.EquipmentChanged] so add/edit/delete done in
 * sheets / detail screens propagate without manual pull-to-refresh.
 */
@HiltViewModel
class GarageViewModel @Inject constructor(
    private val equipmentRepo: EquipmentRepository,
    private val sessionStore: SessionStore,
    events: AppEventBus,
) : ViewModel() {

    /** Defense-in-depth flag for [GarageListScreen]'s sign-in guard. The
     *  outer [MainTabsScreen] already gates the whole tab on auth, but the
     *  screen reads this so deep-link / preview entry points still render
     *  the friendly sign-in prompt instead of bouncing through a 401. */
    val isAuthenticated: Boolean get() = sessionStore.isAuthenticated

    private val _state = MutableStateFlow<RemoteState<List<Equipment>>>(RemoteState.Idle)
    val state: StateFlow<RemoteState<List<Equipment>>> = _state.asStateFlow()

    var deleteError by mutableStateOf<ApiError?>(null)

    init {
        viewModelScope.launch {
            events.events.filterIsInstance<DomainEvent.EquipmentChanged>().collect {
                load(forceRefresh = true)
            }
        }
    }

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh) _state.value = RemoteState.Loading
            try {
                val list = equipmentRepo.fetchEquipment()
                _state.value = if (list.isEmpty()) {
                    RemoteState.Empty("garage.empty")
                } else {
                    RemoteState.Loaded(list)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiError) {
                _state.value = RemoteState.Failed(e)
            } catch (e: Exception) {
                _state.value = RemoteState.Failed(ApiError.from(e))
            }
        }
    }

    fun findEquipment(id: String): Equipment? =
        (_state.value as? RemoteState.Loaded)?.value?.firstOrNull { it.id == id }

    fun deleteEquipment(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            deleteError = null
            try {
                equipmentRepo.deleteEquipment(id)
                onSuccess()
            } catch (e: ApiError) {
                deleteError = e
            } catch (e: Exception) {
                deleteError = ApiError.from(e)
            }
        }
    }

    fun clearDeleteError() { deleteError = null }
}
