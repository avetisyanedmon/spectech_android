# 01 — Secure Storage (Keychain replacement)

iOS source: `SpecTechIOS/Infrastructure/Storage/KeychainStore.swift`.

iOS persists three pieces of sensitive data in Keychain:

| Key | Content | iOS code |
|---|---|---|
| `auth_session` | JSON-encoded `AuthSession` | `SessionStore` |
| `local_profile` | JSON-encoded `LocalProfile` | `ProfileStore` |
| Anything else app-level | n/a (only those two today) | — |

iOS uses `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` — the device must be
unlocked to read, and the data does not migrate to a new device.

## Android equivalent: EncryptedSharedPreferences

Hardware-backed when possible (StrongBox on supported devices), AES-256
encryption of values and AES-SIV of keys.

```kotlin
interface SecureStorage {
    fun read(key: String): String?
    fun save(key: String, value: String)
    fun delete(key: String)
}

class EncryptedSecureStorage @Inject constructor(
    @ApplicationContext private val ctx: Context,
) : SecureStorage {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            ctx,
            "spectech_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun read(key: String): String? = prefs.getString(key, null)

    override fun save(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun delete(key: String) {
        prefs.edit().remove(key).apply()
    }
}
```

Add to `core/platform/di/PlatformModule.kt`:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {
    @Binds @Singleton
    abstract fun bindSecureStorage(impl: EncryptedSecureStorage): SecureStorage
}
```

## Dependencies

```kotlin
// gradle/libs.versions.toml
[libraries]
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version = "1.1.0-alpha06" }
```

`1.1.0` is the modern alpha that supports the `MasterKey` API; `1.0.0`
stable uses the deprecated `MasterKeys` helper. Either works; prefer the
newer one.

## Threat model parity

| iOS behavior | Android equivalent |
|---|---|
| Data only readable when device is unlocked | EncryptedSharedPreferences uses the device's KeyStore; data is bound to the device's lock-screen credentials when StrongBox is available |
| Data NOT migrated to a new device | Tied to the local KeyStore — wiping the app or uninstalling deletes the key |
| Survives app restart | Yes |
| Inaccessible to other apps (sandbox) | Yes — Android app sandbox |
| User can clear by removing app | Yes |

## What NOT to put here

- The HMAC client secret — that's `BuildConfig`, not user-specific.
- Notification history — that's not sensitive; use Preferences DataStore
  (cleartext is acceptable; no PII beyond order IDs).
- Image cache — Coil disk cache; never sensitive.

## Backup exclusion

By default Android backs up SharedPreferences via Auto Backup. Exclude the
encrypted prefs file so the encrypted blob (which won't decrypt on a new
device anyway) doesn't bloat the backup:

```xml
<!-- AndroidManifest.xml -->
<application android:dataExtractionRules="@xml/backup_rules" …>

<!-- res/xml/backup_rules.xml -->
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="sharedpref" path="spectech_secure.xml"/>
    </cloud-backup>
    <device-transfer>
        <exclude domain="sharedpref" path="spectech_secure.xml"/>
    </device-transfer>
</data-extraction-rules>
```

iOS sets `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` which already
excludes the key from iCloud backups; this is the Android parity.

## Migration plan if you ever change the encryption scheme

Don't read keys you wrote with a different scheme — `EncryptedSharedPreferences`
constructor takes the scheme as a parameter. If you ever change it:
1. Try to read with the old scheme.
2. If you can, copy values to the new scheme.
3. Delete the old file.

The shipping app has no such migration today.
