# 01 — Auth

iOS sources:
- `SpecTechIOS/Features/Auth/AuthService.swift`
- `SpecTechIOS/Features/Auth/AuthFlow.swift` (`AuthFlowViewModel`, `StartAuthView`, `VerifyOTPView`)
- `SpecTechIOS/Features/Auth/AuthSheetView.swift`
- `SpecTechIOS/Features/Auth/RegisterView.swift`
- `SpecTechIOS/Features/Auth/RussianPhoneFormatter.swift`
- `SpecTechIOS/App/SessionStore.swift`

## Flow

```
              ┌── "Sign In" CTA ──────────────────────┐
              ▼                                       │
        StartAuth (phone)                             │
              │                       "Create account"│
              ▼                                       │
   POST /auth/send-otp                                │
              │                                       │
              ▼                                       │
       VerifyOtp (6 digits)  ◀── 60-second resend ─── │
              │                                       │
              ▼                                       │
   POST /auth/verify-otp  ──► AuthSession persisted   │
              │                                       │
              ▼                                       │
   Sheet dismisses, RootView re-renders               │
                                                      ▼
                                                Register form
                                                      │
                                                      ▼
                                          (phone field reuses formatter)
                                                      │
                                                      ▼
                                   onSendOtp → set registrationProfile,
                                              submitPhone → goes to VerifyOtp
```

The Auth flow is presented as a **modal sheet** with its own nested
`NavigationStack`. Three internal destinations: `start`, `register`, `verifyOtp`.

## ViewModel

```kotlin
@HiltViewModel
class AuthFlowViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val phoneNormalizer: PhoneNormalizer,
) : ViewModel() {

    var startError by mutableStateOf<ApiError?>(null)
    var verifyError by mutableStateOf<ApiError?>(null)
    var isSubmittingPhone by mutableStateOf(false)
    var isVerifyingCode by mutableStateOf(false)

    // Trigger to advance navigation to verifyOtp; observer collects and consumes
    private val _pendingVerifyPhone = MutableStateFlow<String?>(null)
    val pendingVerifyPhone: StateFlow<String?> = _pendingVerifyPhone.asStateFlow()

    var registrationProfile: RegistrationProfile? = null

    fun submitPhone(phone: String) = viewModelScope.launch {
        isSubmittingPhone = true
        runCatching { authRepo.startAuth(phone) }
            .onSuccess { normalized ->
                startError = null
                _pendingVerifyPhone.value = normalized
            }
            .onFailure { e -> startError = ApiError.from(e) }
        isSubmittingPhone = false
    }

    fun submitCode(phone: String, code: String) = viewModelScope.launch {
        isVerifyingCode = true
        runCatching { authRepo.verifyAuth(phone, code, registrationProfile) }
            .onSuccess { verifyError = null }
            .onFailure { e -> verifyError = ApiError.from(e) }
        isVerifyingCode = false
    }

    fun consumePendingVerifyPhone() { _pendingVerifyPhone.value = null }
}
```

## Repository

```kotlin
class AuthRepository @Inject constructor(
    private val api: ApiClient,
    private val sessionStore: SessionStore,
    private val phoneNormalizer: PhoneNormalizer,
) {
    suspend fun startAuth(rawPhone: String): String {
        val phone = phoneNormalizer.normalizeRussian(rawPhone)
        val env = api.send<SendOtpResponseData>(AuthApi.SendOtp(SendOtpBody(phone)))
        return env.data.phone
    }

    suspend fun verifyAuth(phone: String, code: String, profile: RegistrationProfile?): AuthSession {
        val env = api.send<VerifyOtpResponseData>(AuthApi.VerifyOtp(VerifyOtpBody(
            phone = phone,
            code = code,
            name = profile?.name,
            email = profile?.email?.takeIf { it.isNotEmpty() },
            city = profile?.city,
            role = profile?.role?.wire,
            accountType = profile?.accountType?.wire,
        )))
        val data = env.data
        val session = AuthSession(
            token = data.token,
            user = User(
                id = Uuid.parse(data.user.id),
                phone = data.user.phone,
                role = UserRole.entries.firstOrNull { it.wire == data.user.role } ?: UserRole.CUSTOMER,
                name = data.user.name,
                email = data.user.email,
                city = data.user.city,
                createdAt = data.user.createdAt?.let { Instant.parse(it) } ?: Clock.System.now(),
            ),
            isNewUser = data.isNewUser,
        )
        sessionStore.save(session)
        return session
    }

    suspend fun logout() = sessionStore.clearSession()
}
```

## Phone normalization

Exact port of iOS `PhoneNormalizer`:

```kotlin
class PhoneNormalizer @Inject constructor() {
    fun normalizeRussian(input: String): String {
        val digits = input.filter { it.isDigit() }
        require(digits.isNotEmpty()) { throw ApiError.InvalidPhone }
        val normalized = when {
            digits.length == 10 -> "+7$digits"
            digits.length == 11 && digits.first() == '8' -> "+7${digits.drop(1)}"
            digits.length == 11 && digits.first() == '7' -> "+$digits"
            else -> throw ApiError.InvalidPhone
        }
        if (normalized.length != 12) throw ApiError.InvalidPhone
        return normalized
    }
}
```

## Phone formatter (display masking)

Port `RussianPhoneFormatter.swift` line-by-line. The format is
`+7 (XXX) XXX-XX-XX`. On Compose:

```kotlin
class RussianPhoneFormatter {
    private var _raw = mutableStateOf("")
    val raw: String get() = _raw.value

    val formatted: String get() {
        if (raw.isEmpty()) return "+7"
        val d = raw
        return buildString {
            append("+7")
            if (d.isNotEmpty()) append(" (").append(d.take(3))
            if (d.length >= 3) append(")")
            if (d.length >= 4) append(" ").append(d.substring(3, minOf(6, d.length)))
            if (d.length >= 7) append("-").append(d.substring(6, minOf(8, d.length)))
            if (d.length >= 9) append("-").append(d.substring(8, minOf(10, d.length)))
        }
    }

    val e164: String? get() = if (raw.length == 10) "+7$raw" else null
    val isComplete: Boolean get() = raw.length == 10

    fun update(newText: String) {
        val previousFormatted = formatted
        val allDigits = newText.filter { it.isDigit() }
        val subscriberDigits = when {
            newText.startsWith("+7") || newText.startsWith("+8") -> allDigits.drop(1)
            allDigits.length >= 11 && allDigits.first() == '8' -> allDigits.drop(1)
            allDigits.length >= 11 && allDigits.first() == '7' -> allDigits.drop(1)
            else -> allDigits
        }
        val next = subscriberDigits.take(10)
        if (next == _raw.value && newText.length < previousFormatted.length) {
            _raw.value = _raw.value.dropLast(1)
        } else {
            _raw.value = next
        }
    }

    fun clear() { _raw.value = "" }
}
```

`RussianPhoneField` composable:

```kotlin
@Composable
fun RussianPhoneField(
    formatter: RussianPhoneFormatter,
    placeholder: String = "+7 (999) 000-00-00",
    modifier: Modifier = Modifier,
) {
    var display by remember { mutableStateOf(formatter.formatted) }
    LaunchedEffect(formatter.formatted) { display = formatter.formatted }
    TextField(
        value = display,
        onValueChange = { display = it; formatter.update(it) },
        placeholder = { Text(placeholder) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = modifier,
    )
}
```

## OTP UI

Six-digit code field. Each box accepts one digit, auto-advances on input,
back-advances on delete. Pasting a 6-digit code distributes across all boxes.

```kotlin
@Composable
fun OtpBoxes(value: String, onValueChange: (String) -> Unit, codeLength: Int = 6) {
    val focusRequesters = remember { List(codeLength) { FocusRequester() } }
    val digits = remember(value) {
        derivedStateOf {
            val chars = value.filter { it.isDigit() }.take(codeLength)
            (0 until codeLength).map { chars.getOrNull(it)?.toString() ?: "" }
        }
    }
    LaunchedEffect(Unit) { focusRequesters[0].requestFocus() }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        digits.value.forEachIndexed { index, digit ->
            BasicTextField(
                value = digit,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() }
                    if (filtered.length >= codeLength) {
                        onValueChange(filtered.take(codeLength))
                        focusRequesters[codeLength - 1].requestFocus()
                        return@BasicTextField
                    }
                    val finalChar = filtered.lastOrNull()?.toString() ?: ""
                    val newDigits = digits.value.toMutableList().also { it[index] = finalChar }
                    onValueChange(newDigits.joinToString(""))
                    if (finalChar.isNotEmpty() && index < codeLength - 1) focusRequesters[index + 1].requestFocus()
                    else if (finalChar.isEmpty() && index > 0) focusRequesters[index - 1].requestFocus()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier
                    .size(48.dp, 56.dp)
                    .focusRequester(focusRequesters[index]),
                singleLine = true,
            )
        }
    }
}
```

Enable Android system OTP autofill by setting on the BasicTextField's
modifier:

```kotlin
.semantics { contentType = ContentType.NewSmsOtpCode }
```

(For older API levels, the `BasicTextField` can also be wrapped with
`Modifier.semantics { autofillType = AutofillType.SmsOtpCode }` — pick the
form available in the Compose version you're on.)

## Resend countdown

iOS uses a `Timer` that decrements `resendCountdown` every second from 60.
Compose:

```kotlin
var secondsRemaining by remember { mutableIntStateOf(60) }
LaunchedEffect(Unit) {
    while (secondsRemaining > 0) {
        delay(1.seconds)
        secondsRemaining--
    }
}
```

## Auth sheet wiring

Two ways to host the auth sheet — pick the one that matches the Navigation
Compose style. Approach 1: a full-screen `Dialog` destination at the root
`NavHost`. Approach 2: a `ModalBottomSheet` rendered from `MainTabsScreen`.

The iOS app uses a sheet (modal, drag-dismissable). Use `ModalBottomSheet`
with `fullScreen = true` (`skipPartiallyExpanded = true`) for the visual
match.

```kotlin
@Composable
fun AuthSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        AuthNavHost(onComplete = onDismiss)
    }
}

@Composable
fun AuthNavHost(onComplete: () -> Unit) {
    val nav = rememberNavController()
    val vm: AuthFlowViewModel = hiltViewModel()

    // When VM signals a pending verify phone, push the verify route.
    val pendingPhone by vm.pendingVerifyPhone.collectAsStateWithLifecycle()
    LaunchedEffect(pendingPhone) {
        if (pendingPhone != null) {
            nav.navigate(AuthRoutes.Verify(pendingPhone!!))
            vm.consumePendingVerifyPhone()
        }
    }

    NavHost(nav, startDestination = AuthRoutes.Start) {
        composable<AuthRoutes.Start> { StartAuthScreen(vm, onCreateAccount = { nav.navigate(AuthRoutes.Register) }) }
        composable<AuthRoutes.Register> { RegisterScreen(vm, onSignIn = { nav.popBackStack(AuthRoutes.Start, false) }) }
        composable<AuthRoutes.Verify> { backStack ->
            val args = backStack.toRoute<AuthRoutes.Verify>()
            VerifyOtpScreen(vm, args.phone)
        }
    }

    // Observe session — when it becomes authenticated, complete the sheet
    val session by hiltViewModel<SessionViewModel>().session.collectAsStateWithLifecycle()
    LaunchedEffect(session) { if (session != null) onComplete() }
}
```

## RegisterView details

Form fields (iOS `RegisterView.swift`):
- Account type segmented picker: Individual / Legal Entity
- Full name (required)
- Email (optional, validate format if present)
- Phone (uses `RussianPhoneFormatter`)
- City (required)

Submit constructs `RegistrationProfile` and calls
`onSendOtp(phone, profile)` which routes through the same `submitPhone`
in the VM — so registration and sign-in share the OTP step.

## Logout

```kotlin
fun logout(scope: CoroutineScope) {
    scope.launch {
        pushRepo.unregister()    // POST /notifications/unregister
        authRepo.logout()        // SessionStore.clearSession
        // Saved-filter store doesn't need clearing — it's per-user on the server
        // and the local mirror is harmless to retain until next sign-in.
    }
}
```
