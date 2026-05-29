package com.spectech.features.marketplace.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectech.data.auth.SessionStore
import com.spectech.data.events.AppEventBus
import com.spectech.data.events.DomainEvent
import com.spectech.data.orders.OrdersRepository
import com.spectech.domain.error.ApiError
import com.spectech.domain.model.Bid
import com.spectech.domain.model.ContractorContact
import com.spectech.domain.model.Order
import com.spectech.features.marketplace.navigation.MarketplaceRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/**
 * Per-detail-screen state container. Mirrors iOS `OrderDetailView`'s @State
 * bag (SpecTechIOS/Scene/Tabs/Marketplace/Order/OrderDetailView.swift). Picks
 * up the `orderId` from the nav route, hydrates from the marketplace cache
 * if available, then issues a fresh fetch so the screen always shows the
 * authoritative server snapshot.
 *
 * Subscribes to [DomainEvent.OrdersChanged] so any mutation made anywhere in
 * the app (bid submit, accept, withdraw, delete) refreshes this screen without
 * forcing the parent list to re-emit.
 *
 * Owns the optimistic state needed by [OrderDetailScreen]:
 *   - [acceptedBidIds] / [acceptedContacts] — survives the server reflecting
 *     the accept (avoids the "Accept Offer" CTA flickering back into view).
 *   - [acceptingBidId] — single-in-flight guard for the accept button spinner.
 *   - [isWithdrawing] / [isDeleting] — single-in-flight for those CTAs.
 *   - [acceptError] / [withdrawError] / [deleteError] — alerts cleared by the
 *     UI when the user dismisses them.
 */
@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val ordersRepo: OrdersRepository,
    private val sessionStore: SessionStore,
    private val events: AppEventBus,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val orderId: String = savedStateHandle.toRouteOrderId()

    private val _order = MutableStateFlow<Order?>(null)
    val order: StateFlow<Order?> = _order.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<ApiError?>(null)
    val loadError: StateFlow<ApiError?> = _loadError.asStateFlow()

    // Optimistic-accept tracking — survives the server lagging behind. Each
    // entry is a bid id the user (or the server) flipped to accepted.
    private val _acceptedBidIds = MutableStateFlow<Set<String>>(emptySet())
    val acceptedBidIds: StateFlow<Set<String>> = _acceptedBidIds.asStateFlow()

    private val _acceptedContacts = MutableStateFlow<Map<String, ContractorContact>>(emptyMap())
    val acceptedContacts: StateFlow<Map<String, ContractorContact>> = _acceptedContacts.asStateFlow()

    var acceptingBidId by mutableStateOf<String?>(null)
        private set
    var acceptError by mutableStateOf<ApiError?>(null)

    var isWithdrawing by mutableStateOf(false)
        private set
    var withdrawError by mutableStateOf<ApiError?>(null)

    var isDeleting by mutableStateOf(false)
        private set
    var deleteError by mutableStateOf<ApiError?>(null)

    private var refreshJob: Job? = null

    init {
        // Refresh on any orders-changed event from anywhere in the app.
        viewModelScope.launch {
            events.events.filterIsInstance<DomainEvent.OrdersChanged>().collect {
                refresh()
            }
        }
        refresh()
    }

    /** Seeds the screen from the parent marketplace list's cached snapshot. */
    fun hydrateFromCache(snapshot: Order?) {
        if (snapshot != null && _order.value == null) {
            _order.value = snapshot
            seedAcceptedFrom(snapshot)
        }
    }

    /**
     * Fetches the order across the multi-scope fallback in
     * [OrdersRepository.fetchOrder]. Keeps the previous order on the screen if
     * the refresh fails or returns null — better than blanking the UI.
     */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _isLoading.value = _order.value == null
            try {
                val fresh = ordersRepo.fetchOrder(orderId)
                if (fresh != null) {
                    _order.value = fresh
                    mergeAcceptedState(fresh)
                    _loadError.value = null
                } else if (_order.value == null) {
                    // Nothing cached and nothing fetched — let the screen surface a "not found" state.
                    _loadError.value = ApiError(
                        code = ApiError.LocalCodes.FALLBACK_404,
                        message = "Order not found.",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiError) {
                if (_order.value == null) _loadError.value = e
            } catch (e: Throwable) {
                if (_order.value == null) _loadError.value = ApiError.from(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Optimistic merge after a bid is submitted — keeps the action area in sync. */
    fun mergeSubmittedBid(bid: Bid) {
        val current = _order.value ?: return
        if (current.bids.any { it.id == bid.id }) return
        _order.value = current.copy(
            bids = current.bids + bid,
            bidCount = current.bidCount + 1,
        )
    }

    fun acceptBid(bid: Bid) {
        val order = _order.value ?: return
        if (acceptingBidId != null) return
        acceptingBidId = bid.id
        acceptError = null
        viewModelScope.launch {
            try {
                val fromApi = ordersRepo.acceptBid(orderId = order.id, bidId = bid.id)
                // Resolve a contact to render: prefer the API response, then the
                // bid's embedded contractor block, then the flat fallback. Never
                // sit on "awaiting contact" once the accept succeeded.
                val resolved = resolveContact(api = fromApi, fromBid = bid)
                _acceptedContacts.value = _acceptedContacts.value + (bid.id to resolved)
                _acceptedBidIds.value = _acceptedBidIds.value + bid.id
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiError) {
                acceptError = e
            } catch (e: Throwable) {
                acceptError = ApiError.from(e)
            } finally {
                acceptingBidId = null
            }
        }
    }

    fun withdrawBid(onSuccess: () -> Unit) {
        val order = _order.value ?: return
        val userId = sessionStore.currentUser?.id ?: return
        val myBid = order.bids.firstOrNull { it.contractorId == userId || it.userId == userId } ?: return
        if (isWithdrawing) return
        isWithdrawing = true
        withdrawError = null
        viewModelScope.launch {
            try {
                ordersRepo.withdrawBid(order.id, myBid.id)
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiError) {
                withdrawError = e
            } catch (e: Throwable) {
                withdrawError = ApiError.from(e)
            } finally {
                isWithdrawing = false
            }
        }
    }

    fun deleteOrder(onSuccess: () -> Unit) {
        val order = _order.value ?: return
        if (isDeleting) return
        isDeleting = true
        deleteError = null
        viewModelScope.launch {
            try {
                ordersRepo.deleteOrder(order.id)
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiError) {
                deleteError = e
            } catch (e: Throwable) {
                deleteError = ApiError.from(e)
            } finally {
                isDeleting = false
            }
        }
    }

    /** Caller is the order owner — current user's id matches `creatorId`. */
    fun isOwnOrder(order: Order): Boolean {
        val userId = sessionStore.currentUser?.id ?: return false
        return order.creatorId?.lowercase() == userId.lowercase()
    }

    /** Current user's bid on this order (contractor side), if any. */
    fun myBid(order: Order): Bid? {
        val userId = sessionStore.currentUser?.id ?: return null
        return order.bids.firstOrNull { it.contractorId == userId || it.userId == userId }
    }

    /** True when the current user's bid has been accepted by the customer. */
    fun isMyBidAccepted(order: Order): Boolean {
        val mine = myBid(order) ?: return false
        return _acceptedBidIds.value.contains(mine.id) || mine.isAccepted
    }

    val isAuthenticated: Boolean get() = sessionStore.isAuthenticated

    // ─── helpers ───────────────────────────────────────────────────────────

    /**
     * Picks up bids that were already accepted on the server side when the
     * screen first hydrates. Never clears local optimistic accepts — once the
     * user has tapped Accept, we keep them flipped until the next refresh
     * confirms or contradicts.
     */
    private fun seedAcceptedFrom(order: Order) {
        val acceptedIds = mutableSetOf<String>()
        val contacts = mutableMapOf<String, ContractorContact>()
        order.bids.filter { it.isAccepted }.forEach { bid ->
            acceptedIds += bid.id
            bid.contractorContact?.let { contacts[bid.id] = it }
        }
        if (acceptedIds.isEmpty()) {
            order.acceptedBidId?.let { legacyId ->
                acceptedIds += legacyId
                order.bids.firstOrNull { it.id == legacyId }?.contractorContact?.let {
                    contacts[legacyId] = it
                }
            }
        }
        if (acceptedIds.isNotEmpty()) {
            _acceptedBidIds.value = _acceptedBidIds.value + acceptedIds
        }
        if (contacts.isNotEmpty()) {
            _acceptedContacts.value = _acceptedContacts.value + contacts
        }
    }

    /** Reconcile locally-tracked accepts with a fresh server snapshot. */
    private fun mergeAcceptedState(fresh: Order) {
        fresh.bids.filter { it.isAccepted }.forEach { bid ->
            _acceptedBidIds.value = _acceptedBidIds.value + bid.id
            val existing = _acceptedContacts.value[bid.id]
            if (existing?.phone.isNullOrEmpty()) {
                bid.contractorContact?.let { resolved ->
                    _acceptedContacts.value = _acceptedContacts.value + (bid.id to resolved)
                }
            }
        }
    }

    private fun resolveContact(api: ContractorContact, fromBid: Bid): ContractorContact {
        val fromBidContact = fromBid.contractorContact
        val apiHasPhone = !api.phone.isNullOrEmpty()
        return when {
            apiHasPhone -> api
            fromBidContact != null -> fromBidContact
            else -> ContractorContact(
                phone = fromBid.contractorPhone,
                name = fromBid.contractorName,
            )
        }
    }
}

/**
 * Extracts the `orderId` route arg out of the nav `SavedStateHandle`. Compose
 * NavType serializes `MarketplaceRoute.Detail.orderId` into the bundle under
 * its property name, so we just read it directly. Falls back to empty to keep
 * the VM constructable in previews / tests.
 */
private fun SavedStateHandle.toRouteOrderId(): String {
    return get<String>("orderId") ?: run {
        // Defensive: route arg names are stable but if the route ever renames,
        // construct the type-safe key from the route class.
        val key = MarketplaceRoute.Detail::orderId.name
        get<String>(key) ?: ""
    }
}
