package com.spectech.domain.enums

/** Query-parameter value sent as `?view=…` on `GET /orders`. */
enum class OrderScope(val wire: String) {
    ALL("all"),
    MINE("mine"),
    MARKETPLACE("marketplace"),
    PENDING("pending");
}
