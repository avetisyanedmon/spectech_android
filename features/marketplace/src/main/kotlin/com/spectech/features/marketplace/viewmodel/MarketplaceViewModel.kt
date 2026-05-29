package com.spectech.features.marketplace.viewmodel

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
import com.spectech.domain.enums.OrderStatus
import com.spectech.domain.error.ApiError
import com.spectech.domain.model.Order
import com.spectech.domain.model.OrderFilters
import com.spectech.domain.state.RemoteState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Powers the Marketplace tab. Mirrors iOS `MarketplaceListViewModel`
 * (SpecTechIOS/Scene/Tabs/Marketplace/MarketplaceListViewModel.swift).
 *
 *   - Initial load on first composition.
 *   - Auto-reload on [DomainEvent.OrdersChanged] (other features mutating
 *     orders re-trigger us — accept-bid, withdraw, create, delete, etc.).
 *   - Auto-reload when the active user changes (sign-in / sign-out flips the
 *     "did you bid on this?" affordances).
 *   - Auto-reload when filters change.
 *   - Pagination: pulls the next page when [loadMoreIfNeeded] is called with
 *     the currently last visible order.
 *
 * Client-side filtering hides orders that are not OPEN or are expired — same
 * as iOS `visibleOrders`.
 */
@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val ordersRepo: OrdersRepository,
    private val sessionStore: SessionStore,
    events: AppEventBus,
) : ViewModel() {

    private val _state = MutableStateFlow<RemoteState<List<Order>>>(RemoteState.Idle)
    val state: StateFlow<RemoteState<List<Order>>> = _state.asStateFlow()

    private val _filters = MutableStateFlow(OrderFilters())
    val filters: StateFlow<OrderFilters> = _filters.asStateFlow()

    var showingFilters by mutableStateOf(false)
    var isLoadingMore by mutableStateOf(false)
        private set
    var hasMorePages by mutableStateOf(true)
        private set
    private var currentOffset: Int = 0
    private val pageSize = OrdersRepository.PAGE_SIZE

    /** Open + non-expired + matching the active filters. */
    val visibleOrders: StateFlow<List<Order>> = combine(_state, _filters) { s, f ->
        when (s) {
            is RemoteState.Loaded -> s.value
                .filter { it.status == OrderStatus.OPEN && !it.isExpired }
                .filter(f::matches)
            else -> emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val isAuthenticated: Boolean get() = sessionStore.isAuthenticated

    /**
     * Live user-id of the active session. Drives the role-dependent
     * affordances on each [OrderCardView] (own order / submitted bid /
     * accepted bid). `null` while signed out. Same `SharingStarted.WhileSubscribed`
     * cadence as [visibleOrders] so the list pauses observation when off-screen.
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
        viewModelScope.launch {
            sessionStore.currentSession
                .map { it?.user?.id }
                .distinctUntilChanged()
                .drop(1)
                .collect { load(forceRefresh = true) }
        }
        viewModelScope.launch {
            // Skip the initial value emission; refresh only on subsequent changes.
            _filters.drop(1).collect { load(forceRefresh = true) }
        }
    }

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh) _state.value = RemoteState.Loading
            currentOffset = 0
            hasMorePages = true
            try {
                val orders = ordersRepo.fetchOrders(OrderScope.MARKETPLACE, _filters.value)
                currentOffset = orders.size
                hasMorePages = orders.size >= pageSize
                _state.value = if (orders.isEmpty()) {
                    RemoteState.Empty(EMPTY_MESSAGE_KEY)
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
            val visible = visibleOrders.value
            if (!hasMorePages || isLoadingMore || visible.isEmpty()) return@launch
            val last = visible.last()
            if (currentItem.id != last.id) return@launch

            isLoadingMore = true
            try {
                val next = ordersRepo.fetchOrdersPage(
                    scope = OrderScope.MARKETPLACE,
                    limit = pageSize,
                    offset = currentOffset,
                    filters = _filters.value,
                )
                hasMorePages = next.size >= pageSize
                currentOffset += next.size
                val loaded = _state.value as? RemoteState.Loaded ?: return@launch
                val existingIds = loaded.value.map { it.id }.toSet()
                val deduped = next.filterNot { it.id in existingIds }
                _state.value = RemoteState.Loaded(loaded.value + deduped)
            } catch (_: Exception) {
                hasMorePages = false
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun setFilters(new: OrderFilters) {
        _filters.value = new
    }

    fun findOrder(orderId: String): Order? {
        val loaded = _state.value as? RemoteState.Loaded ?: return null
        return loaded.value.firstOrNull { it.id == orderId }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        // Sentinel key the screen uses to pick a localized empty-state message;
        // the VM is locale-free so it doesn't touch resources.
        const val EMPTY_MESSAGE_KEY = "marketplace.empty"
    }
}
