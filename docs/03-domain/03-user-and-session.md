# 03 — User & Session

## User

```kotlin
@Serializable
data class User(
    @Contextual val id: Uuid,
    val phone: String,
    val role: UserRole,
    val name: String? = null,
    val email: String? = null,
    val city: String? = null,
    @Contextual val createdAt: Instant,
)
```

Direct port of iOS `User` struct. Use `@Contextual` + register
`UuidSerializer` and `InstantIso8601Serializer` in the JSON config.

## AuthSession

```kotlin
@Serializable
data class AuthSession(
    val token: String,
    val user: User,
    val isNewUser: Boolean,
)
```

Port of iOS `AuthSession`. The token is a JWT issued by the backend.

## ContractorInfo

Nested contractor object some backends embed inside bid responses.

```kotlin
@Serializable
data class ContractorInfo(
    @Contextual val id: Uuid? = null,
    val phone: String? = null,
    val name: String? = null,
    val email: String? = null,
)
```

## ContractorContact

Returned by `acceptBid` and `fetchContractor`.

```kotlin
@Serializable
data class ContractorContact(
    val phone: String? = null,
    val name: String? = null,
)
```

## RegistrationProfile

Sent with OTP verification when creating a new account.

```kotlin
data class RegistrationProfile(
    val name: String,
    val email: String?,
    val city: String,
    val role: UserRole,
    val accountType: AccountType,
) {
    enum class AccountType(val wire: String) {
        INDIVIDUAL("individual"),
        LEGAL_ENTITY("legal_entity"),
    }
}
```

Not serializable directly; flattened into `VerifyOtpBody` at the network layer.

## LocalProfile

Persisted in EncryptedSharedPreferences under the key `local_profile`.

```kotlin
@Serializable
data class LocalProfile(
    val displayName: String = "",
    val companyName: String = "",
    val email: String = "",
    val notificationsEnabled: Boolean = true,
    val language: AppLanguage = AppLanguage.ENGLISH,
)
```

Same shape as iOS, same default values.

## Session restore

`SessionStore` owns the in-memory copy and the secure-storage I/O:

```kotlin
@Singleton
class SessionStore @Inject constructor(
    private val secure: SecureStorage,
) : SessionProvider {

    private val _currentSession = MutableStateFlow<AuthSession?>(null)
    val currentSession: StateFlow<AuthSession?> = _currentSession.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    val isAuthenticated: Boolean get() = _currentSession.value != null
    val currentUser: User? get() = _currentSession.value?.user

    override suspend fun authToken(): String? = _currentSession.value?.token
    override suspend fun clearSession() {
        _currentSession.value = null
        secure.delete(KEY_SESSION)
    }

    suspend fun restore() {
        _isRestoring.value = true
        try {
            val json = secure.read(KEY_SESSION) ?: return
            _currentSession.value = Json.decodeFromString<AuthSession>(json)
        } catch (_: Exception) {
            _currentSession.value = null
        } finally {
            _isRestoring.value = false
        }
    }

    suspend fun save(session: AuthSession) {
        _currentSession.value = session
        runCatching {
            val json = Json.encodeToString(session)
            secure.save(KEY_SESSION, json)
        }
        // Keychain failure is non-fatal: in-memory session still works for this run
    }

    suspend fun applyTemporaryBypassSession(phone: String) {
        _currentSession.value = AuthSession(
            token = "dev-bypass-token",
            user = User(
                id = Uuid.parse("00000000-0000-0000-0000-000000000001"),
                phone = phone,
                role = UserRole.CUSTOMER,
                name = null, email = null, city = null,
                createdAt = Clock.System.now(),
            ),
            isNewUser = false,
        )
    }

    companion object { const val KEY_SESSION = "auth_session" }
}
```

This is the line-by-line equivalent of `SpecTechIOS/App/SessionStore.swift`.

## What persists what

| Datum | iOS storage | Android storage | Key |
|---|---|---|---|
| `AuthSession` | Keychain | EncryptedSharedPreferences | `auth_session` |
| `LocalProfile` | Keychain | EncryptedSharedPreferences | `local_profile` |
| `OrderFilters` (saved) | UserDefaults | DataStore | `spectech.savedFilter.v1` |
| Saved-filter notifications opt-in | UserDefaults | DataStore | `spectech.savedFilter.notificationsEnabled.v1` |
| `AppNotification` history | UserDefaults | DataStore | `app_notifications` |
| Withdrawn bids cache | UserDefaults | DataStore | (see Garage feature) |

The Keychain `service` value on iOS is `com.spectech.ios`. Android can use the
package name `ru.spectech.android` as a logical equivalent — these don't need
to match between platforms; each device has its own secure store.
