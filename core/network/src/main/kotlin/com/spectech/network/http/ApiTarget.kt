package com.spectech.network.http

/**
 * Endpoint description. Mirrors iOS `APITarget` protocol.
 *
 * `body` is a pre-serialized JSON string (or null). Concrete targets use
 * [SpecTechJson] to encode their request payload. This keeps ApiTarget itself
 * free of generic type-parameter machinery and matches how the iOS layer
 * passes pre-encoded `Data` blobs through to URLSession.
 */
interface ApiTarget {
    val path: String
    val method: HttpMethod
    val queryItems: List<Pair<String, String>> get() = emptyList()
    val body: String? get() = null
    val requiresAuth: Boolean get() = true
    val contentType: String? get() = null
}
