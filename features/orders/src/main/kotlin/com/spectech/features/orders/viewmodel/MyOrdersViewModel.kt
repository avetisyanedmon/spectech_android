package com.spectech.features.orders.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectech.data.auth.SessionStore
import com.spectech.data.events.AppEventBus
import com.spectech.data.events.DomainEvent
import com.spectech.data.orders.OrdersRepository
import com.spectech.domain.enums.OrderScope
import com.spectech.domain.error.ApiError
import com.spectech.domain.model.ContractorContact
import com.spectech.domain.model.Order
import com.spectech.domain.state.RemoteState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Customer's own orders. Mirrors iOS `MyOrdersViewModel`
 * (SpecTechIOS/Scene/Tabs/MyOrders/MyOrdersViewModel.swift).
 *
 *   - Loads `?view=mine` (auth required, paged 50/page)
 *   - Auto-reload on [DomainEvent.OrdersChanged]
 *   - [acceptBid] hands the result to the screen so the freshly revealed
 *     contractor contact can be cached locally for inline display until the
 *     orders-changed event arrives and refetches.
 */
@HiltViewModel
class MyOrdersViewModel @Inject constructor(
    private val ordersRepo: OrdersRepository,
    private val sessionStore: SessionStore,
    events: AppEventBus,
) : ViewModel() {

    private val _state = MutableStateFlow<RemoteState<List<Order>>>(RemoteState.Idle)
    val state: StateFlow<RemoteState<List<Order>>> = _state.asStateFlow()

    var isLoadingMore by mutableStateOf(false)
        private set
    var hasMorePages by mutableStateOf(true)
        private set
    private var currentOffset: Int = 0
    private val pageSize = OrdersRepository.PAGE_SIZE

    var acceptError by mutableStateOf<ApiError?>(null)
    var revealedContacts by mutableStateOf<Map<String, ContractorContact>>(emptyMap())
        private set

    /**
     * Live user-id of the active session. Drives the role-dependent affordances
     * on each [com.spectech.features.marketplace.ui.components.OrderCardView]
     * (own-order branch / accepted-bid badge). Mirrors [MarketplaceViewModel]'s
     * `currentUserId` so the My Orders list can show the same affordances.
     */
    val currentUserId: StateFlow<String?> = sessionStore.currentSession
        .map { it?.user?.id }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    init {
        viewModelScope.launch {
            events.events.filterIsInstance<DomainEvent.OrdersChanged>().collect {
                load(forceRefresh = true)
            }
        }
    }

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh) _state.value = RemoteState.Loading
            currentOffset = 0
            hasMorePages = true
            try {
                val orders = ordersRepo.fetchOrders(OrderScope.MINE)
                currentOffset = orders.size
                hasMorePages = orders.size >= pageSize
                _state.value = if (orders.isEmpty()) {
                    RemoteState.Empty("my_orders.empty")
                } else {
                    RemoteState.Loaded(orders)
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

    fun loadMoreIfNeeded(currentItem: Order) {
        viewModelScope.launch {
            val loaded = _state.value as? RemoteState.Loaded ?: return@launch
            if (!hasMorePages || isLoadingMore || loaded.value.isEmpty()) return@launch
            if (currentItem.id != loaded.value.last().id) return@launch

            isLoadingMore = true
            try {
                val next = ordersRepo.fetchOrdersPage(
                    scope = OrderScope.MINE,
                    limit = pageSize,
                    offset = currentOffset,
                )
                hasMorePages = next.size >= pageSize
                currentOffset += next.size
                val existingIds = loaded.value.map { it.id }.toSet()
                _state.value = RemoteState.Loaded(loaded.value + next.filterNot { it.id in existingIds })
            } catch (_: Exception) {
                hasMorePages = false
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun findOrder(orderId: String): Order? =
        (_state.value as? RemoteState.Loaded)?.value?.firstOrNull { it.id == orderId }

    fun acceptBid(orderId: String, bidId: String) {
        viewModelScope.launch {
            acceptError = null
            try {
                val contact = ordersRepo.acceptBid(orderId, bidId)
                revealedContacts = revealedContacts + (bidId to contact)
            } catch (e: ApiError) {
                acceptError = e
            } catch (e: Exception) {
                acceptError = ApiError.from(e)
            }
        }
    }

    fun clearAcceptError() { acceptError = null }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
