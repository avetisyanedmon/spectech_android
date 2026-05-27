# 09 — Profile

iOS sources:
- `SpecTechIOS/Features/Profile/ProfileView.swift`
- `SpecTechIOS/Features/Profile/EditProfileView.swift`
- `SpecTechIOS/Features/Profile/ProfileStore.swift`

## Two states

1. **Not signed in** → show `SignInPromptView` with a "Sign In" CTA, plus
   Privacy Policy and Terms links at the bottom.
2. **Signed in** → show profile card + menu + logout button.

## Profile card

Fields surfaced from `User`:
- Display name (`user.name`)
- Phone (`user.phone`, formatted as `+7 (XXX) XXX-XX-XX`)
- Email (`user.email`, if set)
- City (`user.city`, if set)
- Role badge (Customer / Contractor / Admin)

## Menu items (iOS)

| Menu item | Action |
|---|---|
| Edit profile | Opens `EditProfileView` sheet |
| Language | Opens picker (English / Russian) |
| Notifications | Toggle in `LocalProfile.notificationsEnabled` (purely local — server side toggle is in Saved Filters) |
| Privacy Policy | `openURL("https://www.spectechweb.ru/privacy")` |
| Terms of Service | `openURL("https://www.spectechweb.ru/terms")` |

## ProfileStore (local-only state)

Persists `LocalProfile` in EncryptedSharedPreferences under the key
`local_profile`. Fields:

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

```kotlin
@Singleton
class ProfileStore @Inject constructor(
    private val secure: SecureStorage,
) {
    private val _profile = MutableStateFlow(load())
    val profile: StateFlow<LocalProfile> = _profile.asStateFlow()

    private fun load(): LocalProfile = secure.read("local_profile")
        ?.let { runCatching { Json.decodeFromString<LocalProfile>(it) }.getOrNull() }
        ?: LocalProfile()

    suspend fun update(transform: (LocalProfile) -> LocalProfile) {
        val next = transform(_profile.value)
        _profile.value = next
        secure.save("local_profile", Json.encodeToString(next))
    }
}
```

## EditProfileView

Three editable fields: name, email, city. Submit calls
`PATCH /users/me/profile` (`ProfileAPI.updateProfile`). The response is
the updated user — on success update both `SessionStore.currentSession.user`
and `ProfileStore`.

```kotlin
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val profileRepo: ProfileRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {
    var name by mutableStateOf(sessionStore.currentUser?.name.orEmpty())
    var email by mutableStateOf(sessionStore.currentUser?.email.orEmpty())
    var city by mutableStateOf(sessionStore.currentUser?.city.orEmpty())
    var isSubmitting by mutableStateOf(false); private set
    var error by mutableStateOf<ApiError?>(null)
    var success by mutableStateOf(false); private set

    fun save() = viewModelScope.launch {
        isSubmitting = true
        runCatching { profileRepo.updateProfile(name.trim(), email.trim(), city.trim()) }
            .onSuccess { updatedUser ->
                sessionStore.updateUser(updatedUser)
                success = true
                error = null
            }
            .onFailure { error = ApiError.from(it) }
        isSubmitting = false
    }
}
```

`ProfileRepository`:

```kotlin
class ProfileRepository @Inject constructor(private val api: ApiClient) {
    suspend fun updateProfile(name: String, email: String, city: String): User {
        val body = UpdateProfileBody(name = name, email = email, city = city)
        val env = api.send<UpdateProfileResponse>(ProfileApi.UpdateProfile(body))
        // Map response.user → domain User
        return env.data.toUser()
    }
}
```

`SessionStore.updateUser(...)`:

```kotlin
suspend fun updateUser(user: User) {
    val session = _currentSession.value ?: return
    val next = session.copy(user = user)
    save(next)
}
```

## Language switching

iOS uses `String(localized:)` against the system locale; user picks language
via Profile, value stored in `LocalProfile.language`. The iOS UI re-renders
when language changes because the strings table reads the current locale.

On Android, the modern path is **per-app locale**:

```kotlin
fun setLanguage(language: AppLanguage) {
    val locale = LocaleListCompat.forLanguageTags(language.locale)
    AppCompatDelegate.setApplicationLocales(locale)
}
```

This survives process death and respects the user's choice across launches.
Persist to `ProfileStore` so the picker stays in sync.

Manifest opt-in (gradle plugin handles strings.xml-based detection
automatically since AGP 7.4):

```xml
<application
    android:localeConfig="@xml/locales_config" …>
```

```xml
<!-- res/xml/locales_config.xml -->
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="en" />
    <locale android:name="ru" />
</locale-config>
```

## Logout

```kotlin
fun logout() = viewModelScope.launch {
    showingConfirmation = false
    pushRepo.unregister()
    sessionStore.clearSession()
    // sheet/screen will react to currentSession becoming null
}
```

The confirmation dialog uses Material 3 `AlertDialog` with destructive
button styling (red `confirmButton`).

## Privacy / Terms URLs

```kotlin
val ctx = LocalContext.current
TextButton(onClick = {
    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.spectechweb.ru/privacy")))
}) { Text(stringResource(R.string.privacy_policy)) }
```

## Version footer

iOS shows `"SpecTech Marketplace v1.0.0"` — read from `BuildConfig.VERSION_NAME`.
