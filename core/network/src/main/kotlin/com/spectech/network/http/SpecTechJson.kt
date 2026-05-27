package com.spectech.network.http

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

/**
 * Single Json configuration shared by every wire interaction. Mirrors the
 * iOS APIClient's combined encoder/decoder configuration:
 *
 *   - SnakeCase ↔ camelCase conversion (`JSONDecoder.convertFromSnakeCase`)
 *   - ignore unknown fields (lenient over time as the backend evolves)
 *   - omit defaults / nulls on encode (matches iOS `JSONEncoder` behaviour)
 *
 * Concrete `ApiTarget` implementations use this to pre-serialize request
 * bodies so the wire bytes are deterministic before the HMAC interceptor
 * hashes them.
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
val SpecTechJson: Json = Json {
    namingStrategy = JsonNamingStrategy.SnakeCase
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = false
    explicitNulls = false
    isLenient = true
}
