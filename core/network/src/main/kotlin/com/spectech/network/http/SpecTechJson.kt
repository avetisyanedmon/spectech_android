package com.spectech.network.http

import kotlinx.serialization.json.Json

/**
 * Single Json configuration shared by every wire interaction. Mirrors the
 * iOS APIClient's combined encoder/decoder configuration:
 *
 *   - Keys are passed through verbatim. The backend (spectech_backoffice) ships
 *     camelCase for every field — request bodies and response payloads alike
 *     (`ownerId`, `createdAt`, `accountType`, `isNewUser`, etc.) — and the iOS
 *     `JSONEncoder` runs with default key encoding (no convertToSnakeCase),
 *     so iOS encodes its Swift properties verbatim too. We previously had
 *     `JsonNamingStrategy.SnakeCase` here, which caused kotlinx-serialization
 *     to look for `owner_id` / `created_at` / `account_type` on the wire and
 *     either fall back to defaults silently (for nullable fields) or fail
 *     decoding with "Field is required but missing" for required multi-word
 *     fields. The Garage screen was the first hard crash because `Equipment`
 *     has three required `Instant`/`String` fields whose Kotlin names are
 *     multi-word (`ownerId`, `createdAt`, `updatedAt`).
 *   - On the decode side, iOS' `convertFromSnakeCase` is forgiving — it only
 *     converts keys containing underscores and passes camelCase through
 *     unchanged. kotlinx-serialization has no equivalent half-and-half mode,
 *     so we drop the strategy entirely and rely on the fact that every wire
 *     field matches the Kotlin property name exactly.
 *   - ignore unknown fields (lenient over time as the backend evolves)
 *   - omit defaults / nulls on encode (matches iOS `JSONEncoder` behaviour)
 *
 * Concrete `ApiTarget` implementations use this to pre-serialize request
 * bodies so the wire bytes are deterministic before the HMAC interceptor
 * hashes them.
 */
val SpecTechJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = false
    explicitNulls = false
    isLenient = true
}
