# 04 — App Configuration

iOS source: `SpecTechIOS/Infrastructure/Support/AppConfiguration.swift`.

Holds:
- `apiBaseURL` — `https://spectech-backoffice.onrender.com/api`
- `clientId` — `"ios-app"`
- `clientSecret` — `"66ff056ee8fa15b144a54ab472222b0a7534fe16286d1cfb893f6495fe65be96"`
- `bypassAuthFlow` — debug-only flag to skip auth
- `bypassPhone`, `bypassCode` — debug stubs

## Android: BuildConfig

Use Gradle `buildConfigField` for per-variant values. This is the standard
Android pattern and keeps secrets out of `strings.xml` (which would be
trivially extractable from the APK).

```kotlin
// app/build.gradle.kts
android {
    defaultConfig {
        buildConfigField("String", "API_BASE_URL", "\"https://spectech-backoffice.onrender.com/api\"")
        buildConfigField("String", "API_CLIENT_ID", "\"android-app\"")
        // Coordinate the platform-suffix change with the backend; if backend
        // doesn't differentiate, keep "ios-app" so HMAC verification passes.
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_CLIENT_SECRET", "\"${getSecretFromEnv()}\"")
            buildConfigField("boolean", "BYPASS_AUTH_FLOW", "false")
            buildConfigField("String", "BYPASS_PHONE", "\"+79990000000\"")
            buildConfigField("String", "BYPASS_CODE", "\"111111\"")
            buildConfigField("boolean", "PIN_CERTIFICATES", "false")
        }
        release {
            buildConfigField("String", "API_CLIENT_SECRET", "\"${getSecretFromEnv()}\"")
            buildConfigField("boolean", "BYPASS_AUTH_FLOW", "false")
            buildConfigField("String", "BYPASS_PHONE", "\"\"")
            buildConfigField("String", "BYPASS_CODE", "\"\"")
            buildConfigField("boolean", "PIN_CERTIFICATES", "true")
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

fun getSecretFromEnv(): String {
    // Source of truth: a local.properties file (gitignored) OR a Gradle property.
    val props = Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }
    return props.getProperty("apiClientSecret")
        ?: System.getenv("SPECTECH_API_CLIENT_SECRET")
        ?: ""
}
```

`local.properties` contains the secret in plaintext (gitignored by default).
CI sets `SPECTECH_API_CLIENT_SECRET` as an environment variable / secret.

## AppConfiguration class

```kotlin
@Singleton
class AppConfiguration @Inject constructor() {
    val apiBaseUrl: String = BuildConfig.API_BASE_URL
    val clientId: String = BuildConfig.API_CLIENT_ID
    val clientSecret: String = BuildConfig.API_CLIENT_SECRET
    val bypassAuthFlow: Boolean = BuildConfig.BYPASS_AUTH_FLOW
    val bypassPhone: String = BuildConfig.BYPASS_PHONE
    val bypassCode: String = BuildConfig.BYPASS_CODE
    val pinCertificates: Boolean = BuildConfig.PIN_CERTIFICATES
}
```

(Single source of injectable config — never read `BuildConfig` directly from
random files. Keeps tests easier.)

## Build variants

If staging/production split is needed later:

```kotlin
android {
    productFlavors {
        flavorDimensions += "environment"
        create("staging") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"https://spectech-staging.onrender.com/api\"")
        }
        create("production") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"https://spectech-backoffice.onrender.com/api\"")
        }
    }
}
```

## Secret-handling rationale

The HMAC client secret is shipped in cleartext in the iOS binary too —
extracting it from either binary requires only static analysis. **This is
intentional**: the secret is a per-platform shared signing key, not a
per-user secret. Its purpose is to gate API access to "anyone who has the
app", not "anyone with the secret".

Don't try to hide it harder than this. Code-only obfuscation has been
broken many times; the security model relies on:
1. HMAC + nonce + timestamp → request replay protection
2. JWT auth bound to user phone → per-user authorization
3. Certificate pinning → MITM protection on the wire

Just keep it out of git history (use `local.properties` + env vars).

## Reading the version name / code

```kotlin
val versionName = BuildConfig.VERSION_NAME       // from defaultConfig.versionName
val versionCode = BuildConfig.VERSION_CODE       // from defaultConfig.versionCode
```

Use `versionName` in the Profile footer (`"SpecTech Marketplace v$versionName"`).
