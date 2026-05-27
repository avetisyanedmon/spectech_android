package com.spectech.network.endpoints

import com.spectech.domain.enums.OrderScope
import com.spectech.domain.model.CreateBidRequest
import com.spectech.domain.model.CreateOrderRequest
import com.spectech.domain.model.OrderFilters
import com.spectech.network.http.ApiTarget
import com.spectech.network.http.HttpMethod
import com.spectech.network.http.SpecTechJson
import kotlinx.serialization.encodeToString

/**
 * `/orders` endpoint family. Phase 6 adds [CreateOrder]; delete + bid
 * endpoints arrive with their respective feature phases.
 *
 * Mirrors iOS `OrdersAPI` in SpecTechIOS/Networking/Endpoints/OrdersAPI.swift.
 */
sealed interface OrdersApi : ApiTarget {

    data class FetchOrders(
        val scope: OrderScope,
        val limit: Int = DEFAULT_PAGE_SIZE,
        val offset: Int = 0,
        val filters: OrderFilters? = null,
    ) : OrdersApi {
        override val path: String = "orders"
        override val method: HttpMethod = HttpMethod.GET
        // Marketplace is public; the other scopes require auth. The Ktor
        // plugin in ApiClient still attaches a bearer token when one is
        // available — it just won't error out if there's no session.
        override val requiresAuth: Boolean get() = scope != OrderScope.MARKETPLACE

        override val queryItems: List<Pair<String, String>>
            get() = buildList {
                add("view" to scope.wire)
                add("limit" to limit.toString())
                add("offset" to offset.toString())
                filters?.takeUnless { it.isEmpty }?.let { f ->
                    f.categories.sortedBy { it.wire }.forEach { add("categories" to it.wire) }
                    f.regions.sorted().forEach { add("regions" to it) }
                    f.selectedCities.sorted().forEach { add("cities" to it) }
                    f.pricingUnits.sortedBy { it.wire }.forEach { add("pricingUnits" to it.wire) }
                    f.paymentTypes.sortedBy { it.wire }.forEach { add("paymentTypes" to it.wire) }
                }
            }
    }

    /** Creates a new rental order. Returns the freshly minted [com.spectech.domain.model.Order]. */
    data class CreateOrder(val request: CreateOrderRequest) : OrdersApi {
        override val path: String = "orders"
        override val method: HttpMethod = HttpMethod.POST
        override val requiresAuth: Boolean = true
        override val body: String = SpecTechJson.encodeToString(request)
    }

    /**
     * Contractor submits a bid on an order. Backend returns the persisted
     * [com.spectech.domain.model.Bid]. 402 / `DEPOSIT_REQUIRED` indicates the
     * equipment unit hasn't paid its performance deposit yet — the caller
     * (BidSheetViewModel) translates that into a user-friendly CTA.
     */
    data class SubmitBid(val orderId: String, val request: CreateBidRequest) : OrdersApi {
        override val path: String = "orders/${orderId.lowercase()}/bids"
        override val method: HttpMethod = HttpMethod.POST
        override val requiresAuth: Boolean = true
        override val body: String = SpecTechJson.encodeToString(request)
    }

    /**
     * Customer accepts a contractor's bid. Backend marks the bid accepted,
     * fires a push to the chosen contractor, and returns their phone/name
     * (a [com.spectech.domain.model.ContractorContact]).
     */
    data class AcceptBid(val orderId: String, val bidId: String) : OrdersApi {
        override val path: String = "orders/${orderId.lowercase()}/bids/${bidId.lowercase()}/accept"
        override val method: HttpMethod = HttpMethod.POST
        override val requiresAuth: Boolean = true
    }

    /** Contractor withdraws their own bid. */
    data class WithdrawBid(val orderId: String, val bidId: String) : OrdersApi {
        override val path: String = "orders/${orderId.lowercase()}/bids/${bidId.lowercase()}"
        override val method: HttpMethod = HttpMethod.DELETE
        override val requiresAuth: Boolean = true
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
    }
}
