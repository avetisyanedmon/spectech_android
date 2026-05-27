# 05 — Logging

iOS source: `SpecTechIOS/Networking/API/APILogger.swift`.

iOS uses `os.Logger` with `subsystem = Bundle.main.bundleIdentifier` and
`category = "Network"`. All log calls are wrapped in `#if DEBUG` so release
builds emit nothing.

## Android: Timber

```kotlin
class SpecTechApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // In release: optionally plant a tree that forwards crashes to
            // a crash reporter (Crashlytics) but skips DEBUG/INFO logs.
            Timber.plant(CrashReportingTree())
        }
    }
}

private class CrashReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < Log.WARN) return
        // Forward to Crashlytics if configured
        // FirebaseCrashlytics.getInstance().recordException(t ?: Exception(message))
    }
}
```

## Network logging

Mirror iOS's request/response/retry log shape. Tag the tree with `"Network"`
so log filtering works:

```kotlin
object NetworkLogger {
    private val logger = Timber.tag("Network")

    private val sensitiveHeaders = setOf("Authorization", "X-Signature")

    fun logRequest(request: HttpRequestBuilder) {
        if (!BuildConfig.DEBUG) return
        val lines = buildList {
            add("➡️ ${request.method.value} ${request.url}")
            request.headers.build().entries().forEach { (k, vs) ->
                if (k !in sensitiveHeaders) add("   $k: ${vs.joinToString()}")
            }
            (request.body as? ByteArray)?.let {
                add("   Body: ${prettyJson(it).take(2048)}")
            }
        }
        logger.d(lines.joinToString("\n"))
    }

    fun logResponse(status: Int, url: String, body: ByteArray, durationMs: Long) {
        if (!BuildConfig.DEBUG) return
        val icon = if (status in 200..299) "✅" else "❌"
        val lines = buildList {
            add("$icon $status $url  (${durationMs} ms)")
            if (body.isNotEmpty()) add("   Body: ${prettyJson(body).take(2048)}")
        }
        if (status in 200..299) logger.d(lines.joinToString("\n"))
        else logger.e(lines.joinToString("\n"))
    }

    fun logRetry(attempt: Int, reason: String, url: String) {
        if (!BuildConfig.DEBUG) return
        logger.i("🔄 Retry $attempt for $url — $reason")
    }

    private fun prettyJson(bytes: ByteArray): String = try {
        val obj = Json.parseToJsonElement(String(bytes))
        Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), obj)
    } catch (_: Exception) {
        String(bytes).take(1024)
    }
}
```

Wire into Ktor as a plugin:

```kotlin
install(Logging) {
    level = if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.NONE
    logger = object : io.ktor.client.plugins.logging.Logger {
        override fun log(message: String) { NetworkLogger.timber.d(message) }
    }
    sanitizeHeader { header -> header in NetworkLogger.sensitiveHeaders }
}
```

Or roll your own request/response interceptor inside `OkHttpClient` to match
the exact iOS log format. The Ktor `Logging` plugin is simpler but less
formatted.

## What NEVER to log

- The HMAC signature header (`X-Signature`)
- The bearer token (`Authorization`)
- The full FCM device token
- Full phone numbers in error messages (mask middle digits if you must log)
- User passwords / OTP codes (there's no password here, but be careful about
  OTP codes during the verify flow — log only "OTP submission attempted")

The `sensitiveHeaders` set above redacts the two HTTP headers. For payload
fields like `code` (OTP), `password` (none in this app, but defensive),
add a custom sanitizer that walks the parsed JSON and replaces values.

## Crash reporting (optional)

If the team enables Crashlytics:

```kotlin
implementation("com.google.firebase:firebase-crashlytics")
```

Wire `CrashReportingTree` to forward exceptions:

```kotlin
FirebaseCrashlytics.getInstance().recordException(t)
```

Set user identifiers (carefully — DO NOT use phone or email):

```kotlin
sessionStore.currentUser?.id?.let { uuid ->
    FirebaseCrashlytics.getInstance().setUserId(uuid.toString())
}
```

(iOS does not currently use a crash reporter — adding it is a net-new
capability for the Android port. Discuss with the team before enabling.)
