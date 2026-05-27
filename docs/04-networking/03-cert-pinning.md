# 03 — Certificate Pinning

iOS reference: `SpecTechIOS/Networking/API/APIClient.swift`, lines 26–76.

## What iOS pins

The intermediate-CA SHA-256 hashes for the Render-hosted backend, base64-encoded:

| Hash (base64) | Certificate |
|---|---|
| `HfwWBfutNY2LyET3bRUgP6ycpcGnn9SFf/ryhk++v5Y=` | Google Trust Services WE1 |
| `drJ7gKWAJ9w88dpo2sFwEO2TmX0LYD4vrb6FASSTtac=` | GTS Root R4 |

Pinning intermediates (not leaves) means the pin survives leaf-certificate
rotation — Render rotates leaves regularly via Let's Encrypt-style automation.

iOS uses `URLSessionDelegate.urlSession(_:didReceive:completionHandler:)` and
compares the DER-encoded certificate SHA-256 against the pinned set,
**failing closed** when nothing matches.

## OkHttp `CertificatePinner`

OkHttp pins are expressed differently — they're keyed on the **SubjectPublicKeyInfo**
SHA-256 (a.k.a. SPKI pin), not the full DER cert hash. The iOS pin hashes the
entire DER-encoded certificate. To stay byte-compatible, **regenerate the
pins as SPKI hashes** for Android. The pin set must cover the same two
intermediates.

```kotlin
fun buildCertificatePinner(): CertificatePinner = CertificatePinner.Builder()
    .add("spectech-backoffice.onrender.com",
        "sha256/<SPKI_HASH_OF_GTS_WE1>",   // regenerate, see below
        "sha256/<SPKI_HASH_OF_GTS_ROOT_R4>"
    )
    .build()
```

### Regenerating the SPKI pins

```bash
echo | openssl s_client -connect spectech-backoffice.onrender.com:443 \
    -servername spectech-backoffice.onrender.com -showcerts 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl rsa -pubin -outform DER 2>/dev/null \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64
```

Run this for **each** intermediate in the chain. The OkHttp pin format is
`sha256/<base64-spki-hash>`.

> If the team wants byte-for-byte parity with iOS (cert hash, not SPKI hash),
> use a custom `X509TrustManager` instead of `CertificatePinner`. The SPKI
> approach is the OkHttp-idiomatic one and offers the same security
> properties; only switch to the custom trust manager if you need the exact
> hashes to interop with another tool.

## Alternative: full DER-cert pin (iOS-parity)

Drop down to a `X509TrustManager` if you want to match iOS exactly:

```kotlin
class DerCertPinTrustManager(
    private val pinned: Set<ByteArray>,
    private val hostname: String,
) : X509TrustManager {
    private val default = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        .apply { init(null as KeyStore?) }
        .trustManagers
        .filterIsInstance<X509TrustManager>()
        .first()

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
        // Run standard validation first
        default.checkServerTrusted(chain, authType)
        // Then enforce our pin
        val matched = chain.any { cert ->
            val sha = MessageDigest.getInstance("SHA-256").digest(cert.encoded)
            pinned.any { it.contentEquals(sha) }
        }
        if (!matched) throw SSLPeerUnverifiedException("Certificate pin mismatch for $hostname")
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) =
        default.checkClientTrusted(chain, authType)
    override fun getAcceptedIssuers(): Array<X509Certificate> = default.acceptedIssuers
}
```

Wire into OkHttp:

```kotlin
val tm = DerCertPinTrustManager(
    pinned = setOf(
        Base64.decode("HfwWBfutNY2LyET3bRUgP6ycpcGnn9SFf/ryhk++v5Y=", Base64.DEFAULT),
        Base64.decode("drJ7gKWAJ9w88dpo2sFwEO2TmX0LYD4vrb6FASSTtac=", Base64.DEFAULT),
    ),
    hostname = "spectech-backoffice.onrender.com",
)
val sslContext = SSLContext.getInstance("TLS").apply { init(null, arrayOf(tm), SecureRandom()) }
engine { config {
    sslSocketFactory(sslContext.socketFactory, tm)
    hostnameVerifier { hostname, _ -> hostname == "spectech-backoffice.onrender.com" }
}}
```

This matches the iOS hashes byte-for-byte. Note: a hostname verifier this
strict will block dev backends — gate it with `BuildConfig.PIN_CERTIFICATES`.

## Recommended approach

- **Production builds**: pin via `CertificatePinner` with SPKI hashes
  (preferred — OkHttp-native, less code).
- **Debug builds**: skip pinning entirely so engineers can MITM-debug with
  Charles/mitmproxy. Use a `BuildConfig` flag:

```kotlin
engine {
    config {
        if (BuildConfig.PIN_CERTIFICATES) certificatePinner(buildCertificatePinner())
    }
}
```

```kotlin
// build.gradle.kts
android {
    buildTypes {
        debug { buildConfigField("boolean", "PIN_CERTIFICATES", "false") }
        release { buildConfigField("boolean", "PIN_CERTIFICATES", "true") }
    }
}
```

iOS does NOT skip pinning in DEBUG — it pins always. Be deliberate about
diverging here; only do so if your team needs MITM debugging.

## Network Security Config

Also configure `res/xml/network_security_config.xml` to disallow cleartext
traffic and (optionally) declare an additional layer of pins for older
Android versions:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    <!-- Domain pinning is OkHttp's job; the system config just enforces TLS -->
</network-security-config>
```

Reference in `AndroidManifest.xml`:

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config" …>
```

## Pin rotation playbook

When Render rotates an intermediate:
1. Backend team announces the new chain.
2. Run the SPKI hashing script against the new chain.
3. Ship a backup release with **both** old and new hashes in `CertificatePinner`.
4. After the cutover, drop the old hash in the next release.

Always carry at least one backup pin in production to avoid bricking the app
on rotation.
