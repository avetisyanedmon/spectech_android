package com.spectech.network.endpoints

import com.spectech.network.http.ApiTarget
import com.spectech.network.http.HttpMethod

/**
 * Public news feed. No auth needed — the marketplace tab shows news to
 * signed-out browsers as well.
 *
 * Response is the array directly: `ApiEnvelope<List<NewsItem>>`.
 */
sealed interface NewsApi : ApiTarget {

    data object Fetch : NewsApi {
        override val path: String = "news"
        override val method: HttpMethod = HttpMethod.GET
        override val requiresAuth: Boolean = false
    }
}
