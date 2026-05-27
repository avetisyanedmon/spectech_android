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
import com.spectech.domain.model.Bid
import com.spectech.domain.model.Order
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
 * Contractor's view of orders they've bid on. Mirrors iOS `MyBidsViewModel`
 * (SpecTechIOS/Scene/Tabs/MyBids/MyBidsViewModel.swift).
 *
 *   - Loads `?view=pending` (auth required, paged 50/page)
 *   - Auto-reload on [DomainEvent.OrdersChanged]
 *   - [withdrawBid] is the only mutation; success refetches via the event
 */
@HiltViewModel
class MyBidsViewModel @Inject constructor(
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

    var withdrawError by mutableStateOf<ApiError?>(null)

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
                val orders = ordersRepo.fetchOrders(OrderScope.PENDING)
                currentOffset = orders.size
                hasMorePages = orders.size >= pageSize
                _state.value = if (orders.isEmpty()) {
                    RemoteState.Empty("my_bids.empty")
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
                    scope = OrderScope.PENDING,
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

    /** Returns the bid the current contractor placed on the given order, if any. */
    fun myBid(order: Order): Bid? {
        val myId = sessionStore.currentUser?.id?.lowercase() ?: return null
        return order.bids.firstOrNull { it.contractorId?.lowercase() == myId || it.userId?.lowercase() == myId }
    }

    fun withdrawBid(orderId: String, bidId: String) {
        viewModelScope.launch {
            withdrawError = null
            try {
                ordersRepo.withdrawBid(orderId, bidId)
            } catch (e: ApiError) {
                withdrawError = e
            } catch (e: Exception) {
                withdrawError = ApiError.from(e)
            }
        }
    }

    fun clearWithdrawError() { withdrawError = null }
}
