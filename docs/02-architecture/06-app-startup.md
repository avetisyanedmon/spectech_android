# 06 — App Startup

## What iOS does at startup

`SpecTechIOSApp.swift`:
1. Creates `SessionStore`, `ProfileStore`, `NewsStore`, `NotificationStore`,
   `SavedFilterStore` as `@State` (single-instance, app-scoped).
2. Wires the `AppDelegate` via `@UIApplicationDelegateAdaptor`.
3. On first appearance (`.task`):
   - Wires `AppDelegate.notificationStore = notificationStore` so the static
     property can deliver navigation requests from a freshly tapped
     notification.
   - Constructs `SavedFilterStore(client: APIClient(sessionProvider: sessionStore))`.
   - Awaits `sessionStore.restore()` — reads the persisted session from Keychain.
   - Calls `bootstrapAuthIfNeeded()` — applies the dev bypass session if the
     build flag is on.
   - If authenticated: registers push token + reloads saved filter from server.
   - Requests push permission and (if granted) calls
     `UIApplication.shared.registerForRemoteNotifications()`.
4. `AppDelegate.didFinishLaunchingWithOptions`:
   - Sets `UNUserNotificationCenter.delegate = self`.
   - Configures `SDImageCache` (50 MB memory / 200 MB disk).

## Android startup sequence

### `SpecTechApplication.kt`

```kotlin
@HiltAndroidApp
class SpecTechApplication : Application() {
    @Inject lateinit var notificationStore: NotificationStore  // hold the singleton alive
    @Inject lateinit var sessionStore: SessionStore
    @Inject lateinit var savedFilterStore: SavedFilterStore
    @Inject lateinit var pushRepo: PushRepository
    @Inject lateinit var events: AppEventBus

    override fun onCreate() {
        super.onCreate()
        configureCoil()
        configureNotificationChannel()
        configureLogging()
        // Eager restore of session so MainActivity sees the right auth state
        runBlocking { sessionStore.restore() }
        // Kick off background work — does not block UI
        applicationScope.launch {
            if (sessionStore.isAuthenticated()) {
                pushRepo.registerIfNeeded()
                savedFilterStore.loadFromServer()
            }
        }
    }

    private fun configureCoil() {
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .memoryCache { MemoryCache.Builder().maxSizeBytes(50L * 1024 * 1024).build() }
                .diskCache { DiskCache.Builder().directory(cacheDir.resolve("image_cache"))
                                                .maxSizeBytes(200L * 1024 * 1024).build() }
                .build()
        )
    }

    private fun configureNotificationChannel() {
        // Required on API 26+; one channel per push category if desired
        val channel = NotificationChannel(
            "spectech_default",
            getString(R.string.notification_channel_default),
            NotificationManager.IMPORTANCE_HIGH,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun configureLogging() {
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
```

Register in manifest:

```xml
<application
    android:name=".SpecTechApplication"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:theme="@style/Theme.SpecTech">
    …
</application>
```

### `MainActivity.kt`

Single Activity hosting all of Compose.

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionStore: SessionStore
    @Inject lateinit var notificationStore: NotificationStore

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ignore result */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        setContent {
            SpecTechTheme {
                val isRestoring by sessionStore.isRestoring.collectAsStateWithLifecycle()
                if (isRestoring) {
                    LoadingStateView(R.string.restoring_session)
                } else {
                    SpecTechApp()
                }
            }
        }

        // Handle the case the app was launched by tapping a notification
        handleNotificationIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "spectech") return
        // The NavHost picks up the deep link automatically via composable()'s
        // `deepLinks` registration. Just store the type for tab routing if needed.
        val type = data.getQueryParameter("type")
        val orderId = data.lastPathSegment ?: return
        notificationStore.requestNavigation(NotificationNavigationRequest(
            type = type,
            orderId = orderId,
            offerId = data.getQueryParameter("offerId"),
        ))
    }
}
```

## Session restore

iOS reads `auth_session` from Keychain synchronously in `restore()` (the entire
method runs on `@MainActor` and `keychain.read` is sync). Android needs the
same flow but Keychain replacement (`EncryptedSharedPreferences`) is also sync
— so a single `runBlocking` call in `Application.onCreate` is acceptable for
this one operation (it touches a tiny JSON blob in cleartext-on-disk).

If you prefer not to block, expose the restoration as a `StateFlow<Boolean>`
just like iOS's `isRestoring` and render `LoadingStateView` while it's true.

## What does NOT belong in Application.onCreate

- HTTP calls — those are async; do them inside `applicationScope.launch { … }`.
- FCM token registration — `FirebaseMessaging.getInstance().token` returns a
  task; await it in a coroutine inside `pushRepo.registerIfNeeded()`.
- Workmanager scheduling that isn't needed at first frame.

Keep `Application.onCreate` under ~50 ms to avoid jank on cold start.
