package com.spectech.data.orders

import com.spectech.data.events.AppEventBus
import com.spectech.data.events.DomainEvent
import com.spectech.domain.enums.OrderScope
import com.spectech.domain.model.Bid
import com.spectech.domain.model.ContractorContact
import com.spectech.domain.model.CreateBidRequest
import com.spectech.domain.model.CreateOrderRequest
import com.spectech.domain.model.Order
import com.spectech.domain.model.OrderFilters
import com.spectech.network.endpoints.OrdersApi
import com.spectech.network.http.ApiClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable

/**
 * Wraps `/orders` for both reads and writes. Mirrors iOS `OrdersService`.
 *
 * Mutations emit [DomainEvent.OrdersChanged] on success so live view models
 * (Marketplace, My Orders, My Bids) refresh automatically — the same
 * NotificationCenter-driven pattern iOS uses, but typed.
 */
@Singleton
class OrdersRepository @Inject constructor(
    private val api: ApiClient,
    private val events: AppEventBus,
) {

    /** First page of orders for the given scope/filters. */
    suspend fun fetchOrders(
        scope: OrderScope,
        filters: OrderFilters? = null,
    ): List<Order> = api.send<List<Order>>(
        OrdersApi.FetchOrders(scope = scope, filters = filters),
    ).data

    /** Subsequent page; caller manages the running offset. */
    suspend fun fetchOrdersPage(
        scope: OrderScope,
        limit: Int,
        offset: Int,
        filters: OrderFilters? = null,
    ): List<Order> = api.send<List<Order>>(
        OrdersApi.FetchOrders(
            scope = scope,
            limit = limit,
            offset = offset,
            filters = filters,
        ),
    ).data

    /**
     * Creates a new rental order. Server returns the persisted [Order];
     * subscribers to [DomainEvent.OrdersChanged] (Marketplace + My Orders)
     * reload so the new entry appears immediately.
     */
    suspend fun createOrder(request: CreateOrderRequest): Order {
        val response = api.send<Order>(OrdersApi.CreateOrder(request)).data
        events.emit(DomainEvent.OrdersChanged)
        return response
    }

    /**
     * Contractor submits a bid. Returns the freshly minted [Bid]. The bid
     * surfaces in the order's `bids` array after the next refresh — every
     * subscriber to [DomainEvent.OrdersChanged] reloads, so the customer's
     * "My Orders → Detail" sees the new bid live.
     */
    suspend fun submitBid(orderId: String, request: CreateBidRequest): Bid {
        val response = api.send<Bid>(OrdersApi.SubmitBid(orderId, request)).data
        events.emit(DomainEvent.OrdersChanged)
        return response
    }

    /**
     * Customer accepts a contractor's bid. Returns the chosen contractor's
     * contact (phone, optional name) which the UI reveals on the order
     * detail. The orders list is then refreshed for everyone via the event.
     */
    suspend fun acceptBid(orderId: String, bidId: String): ContractorContact {
        val response = api.send<ContractorContact>(OrdersApi.AcceptBid(orderId, bidId)).data
        events.emit(DomainEvent.OrdersChanged)
        return response
    }

    /** Contractor withdraws their own bid. List subscribers refresh. */
    suspend fun withdrawBid(orderId: String, bidId: String) {
        api.send<WithdrawnBidId>(OrdersApi.WithdrawBid(orderId, bidId))
        events.emit(DomainEvent.OrdersChanged)
    }

    companion object {
        const val PAGE_SIZE: Int = OrdersApi.DEFAULT_PAGE_SIZE
    }
}

/** Backend's withdraw response — just an echo of the deleted id; we discard it. */
@Serializable
private data class WithdrawnBidId(val id: String? = null)
