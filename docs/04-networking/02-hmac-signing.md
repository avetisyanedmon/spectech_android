# 02 — HMAC Request Signing

The backend verifies a per-request HMAC-SHA256 signature on every endpoint.
**This is non-optional** — without correct headers the server returns 401
regardless of the bearer token.

iOS reference: `SpecTechIOS/Networking/API/APIClient.swift`, lines 264–319.

## Headers added per request

| Header | Source |
|---|---|
| `X-Client-Id` | `AppConfiguration.clientId` (currently `"ios-app"` on iOS; use `"android-app"` for Android — **coordinate with backend team first**, or reuse `"ios-app"` if the backend doesn't enforce per-platform IDs) |
| `X-Timestamp` | `Int64(Date().timeIntervalSince1970 * 1000)` — Unix ms as a base-10 string |
| `X-Nonce` | 16 random bytes hex-encoded (lowercase, no separators), so 32 hex chars |
| `X-Signature` | HMAC-SHA256(clientSecret, payload) hex-lowercase |

## Payload format (exact)

```
<METHOD>\n<PATH_AND_QUERY>\n<TIMESTAMP>\n<NONCE>\n<BODY_HASH>
```

- `\n` is a single LF byte (`0x0A`).
- `<METHOD>` is upper-case (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`).
- `<PATH_AND_QUERY>` is the **percent-encoded** path plus, when non-empty,
  `?<percent-encoded-query>`. **No host, no scheme.** Example:
  `/api/orders?view=marketplace&limit=50&offset=0`.
- `<TIMESTAMP>` is the same string put in `X-Timestamp`.
- `<NONCE>` is the same string put in `X-Nonce`.
- `<BODY_HASH>` is SHA-256(body) hex-lowercase. Body rules below.

### Body hash rules — critical

The server only captures `req.rawBody` for `application/json` bodies (via
Express's `express.json` verify hook). For everything else (`multipart/form-data`,
empty GET/DELETE bodies, …) the server hashes the empty string.

iOS reflects this:

```swift
let signableBody: Data = {
    guard let body = urlRequest.httpBody, !body.isEmpty else { return Data() }
    if contentType.lowercased().hasPrefix("application/json") {
        return body
    }
    return Data()
}()
```

The hex SHA-256 of an empty `Data()` is:
`e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`.

The Android plugin must do exactly the same.

## Kotlin Mac plugin

This is the trickiest piece — the signature must be computed **after** the
body has been finalized and the bearer header has been attached. Ktor's
`onRequest` hook fires before the engine serializes, but body conversion
happens in pipelines. Implement as an OkHttp `Interceptor` so we see the
exact bytes that go on the wire:

```kotlin
class HmacInterceptor(
    private val clientId: String,
    clientSecret: String,
) : Interceptor {

    private val secretKey = SecretKeySpec(clientSecret.toByteArray(Charsets.UTF_8), "HmacSHA256")

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val signed = signRequest(original)
        return chain.proceed(signed)
    }

    private fun signRequest(req: Request): Request {
        val timestampMs = System.currentTimeMillis().toString()
        val nonce = randomHex(16)

        val method = req.method.uppercase()
        val pathAndQuery = req.url.encodedPath +
            (req.url.encodedQuery?.takeIf { it.isNotEmpty() }?.let { "?$it" } ?: "")

        val contentType = req.body?.contentType()?.toString().orEmpty().lowercase()
        val signableBody: ByteArray = when {
            req.body == null -> EMPTY_BYTES
            !contentType.startsWith("application/json") -> EMPTY_BYTES
            else -> req.body!!.bytes()  // see note below
        }

        val bodyHash = sha256Hex(signableBody)
        val payload = "$method\n$pathAndQuery\n$timestampMs\n$nonce\n$bodyHash"
        val signature = hmacSha256Hex(payload)

        return req.newBuilder()
            .header("X-Client-Id", clientId)
            .header("X-Timestamp", timestampMs)
            .header("X-Nonce", nonce)
            .header("X-Signature", signature)
            .build()
    }

    private fun hmacSha256Hex(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(payload.toByteArray(Charsets.UTF_8)).toHex()
    }

    private fun sha256Hex(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(data).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun randomHex(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    companion object { private val EMPTY_BYTES = ByteArray(0) }
}
```

### `req.body!!.bytes()` — extension to extract bytes

OkHttp's `RequestBody.writeTo(BufferedSink)` is one-shot, so naively reading
the body consumes it. Use a buffer-and-rewrite pattern:

```kotlin
private fun RequestBody.bytes(): ByteArray {
    val buffer = Buffer()
    writeTo(buffer)
    return buffer.readByteArray()
}
```

…and rebuild the request with the buffered body so the engine can still send
it:

```kotlin
private fun Request.withBufferedJsonBody(): Pair<Request, ByteArray> {
    val body = this.body ?: return this to ByteArray(0)
    val ct = body.contentType()?.toString().orEmpty().lowercase()
    if (!ct.startsWith("application/json")) return this to ByteArray(0)
    val bytes = body.bytes()
    val newBody = bytes.toRequestBody(body.contentType())
    return newBuilder().method(method, newBody).build() to bytes
}
```

Adjust `signRequest` to use this helper. The pattern is standard in OkHttp
HMAC interceptors.

### Hooking the interceptor into Ktor's OkHttp engine

```kotlin
engine {
    config {
        addInterceptor(HmacInterceptor(config.clientId, config.clientSecret))
        certificatePinner(buildCertificatePinner())
    }
}
```

The interceptor sees the **final** serialized body, after the bearer token
has been added by Ktor's `onRequest` hook. That ordering matters: bearer
headers are signed too (the iOS implementation signs `urlRequest` after
setting `Authorization`).

Wait — re-read iOS: it sets the `Authorization` header **before**
`signRequest` is called, but the signature payload **does not include
headers**. Only `METHOD`, `PATH_AND_QUERY`, `TIMESTAMP`, `NONCE`, `BODY_HASH`.

So you do **not** need to worry about the Authorization header for signing
purposes. Just sign in any order relative to bearer attachment.

## Verification checklist

When debugging signature mismatches:

1. ☐ Print the exact payload string the client signed. Each `\n` is a single
   LF byte.
2. ☐ Print the SHA-256 hex of the body. Empty bodies must hash to
   `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`.
3. ☐ Print the path-with-query: it must match what `req.url.encodedPath +
   "?" + req.url.encodedQuery` produces, **percent-encoded exactly once**.
4. ☐ The `X-Timestamp` must be Unix milliseconds, base 10, no decimal point.
5. ☐ The `X-Nonce` must be lowercase hex.
6. ☐ The signature must be lowercase hex.
7. ☐ Multipart uploads must hash an **empty** body — the contentType prefix
   check is what triggers this on iOS, mirror it.

The backend has its own logger; if a request returns 401, check whether the
backend logged "signature mismatch" — that points to one of the items above.
If it logged "stale nonce" or "timestamp drift", check the device's clock
(iOS apparently never had this issue; emulators sometimes drift).

## Client secret

Currently embedded in `AppConfiguration` as a hex string. On Android, store
it in `BuildConfig` (with build types overriding for staging/dev if needed).
This is **NOT a high-trust secret** — the iOS app ships it in cleartext too
— but it does need to be obfuscated against trivial extraction. Use the
Gradle `buildConfigField` approach in
[07-infrastructure/04-app-configuration.md](../07-infrastructure/04-app-configuration.md).
