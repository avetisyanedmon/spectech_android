# 02 — Push Notifications (FCM)

Already extensively covered in [05-features/11-notifications.md](../05-features/11-notifications.md).
This file focuses on the **infrastructure** setup — how to wire FCM into the
project.

## One-time setup

1. Create a Firebase project (or reuse the existing one if backend team has
   one for iOS APNs proxy work).
2. Add an Android app to the project. Use package name `ru.spectech.android`
   (must match `applicationId`).
3. Download `google-services.json` into `app/google-services.json`.
4. Add the Firebase plugin and FCM dependency to Gradle.

### Gradle wiring

`build.gradle.kts` (project):
```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```

`app/build.gradle.kts`:
```kotlin
plugins {
    id("com.google.gms.google-services")
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")
    // Optional: analytics for backend visibility into delivery
    implementation("com.google.firebase:firebase-analytics")
}
```

## Manifest

```xml
<manifest …>
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.INTERNET" />

    <application …>
        <!-- FCM service -->
        <service
            android:name=".platform.push.FcmService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>

        <!-- Default notification channel id (matches what FcmService creates) -->
        <meta-data
            android:name="com.google.firebase.messaging.default_notification_channel_id"
            android:value="spectech_default" />

        <!-- Default small icon for system-tray notifications -->
        <meta-data
            android:name="com.google.firebase.messaging.default_notification_icon"
            android:resource="@drawable/ic_notification" />

        <!-- Default color tint -->
        <meta-data
            android:name="com.google.firebase.messaging.default_notification_color"
            android:resource="@color/brand_blue" />

        <!-- Deep link entry for tap navigation -->
        <activity android:name=".MainActivity" …>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="spectech" android:host="order" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

## Notification channel

Required on API 26+. Create in `SpecTechApplication.onCreate`:

```kotlin
private fun configureNotificationChannel() {
    val channel = NotificationChannel(
        "spectech_default",
        getString(R.string.notification_channel_default),
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = getString(R.string.notification_channel_default_desc)
        enableVibration(true)
        setShowBadge(true)
    }
    getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}
```

For per-category channels (new bid / bid accepted / etc.), create more
channels with friendlier names so the user can tune importance per type in
system settings. The iOS app uses a single category — start with one and add
more if user feedback asks for per-type silencing.

## Hilt + FirebaseMessagingService

`FirebaseMessagingService` is instantiated by Firebase, not by Hilt, so we
can't `@Inject constructor`. Use `@AndroidEntryPoint`:

```kotlin
@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {
    @Inject lateinit var notificationStore: NotificationStore
    @Inject lateinit var pushRepo: PushRepository
    // ... see 05-features/11-notifications.md for body
}
```

## Backend coordination

The backend currently sends:

```js
// services/push.service.js (iOS proxy)
{ token, platform: "ios", … }
```

For Android, the backend must:
1. Accept `platform: "android"` in `POST /notifications/register`.
2. Send the FCM payload in the data-payload form described in
   [05-features/11-notifications.md](../05-features/11-notifications.md).
3. Route to FCM HTTP v1 API or use Firebase Admin SDK.

If the backend team owns push send, file a ticket: "Implement Android push
send via FCM HTTP v1; reuse the existing notification payload schema with
data fields for {type, orderId, offerId, bidId, notificationId, title, body}."

## Token lifecycle (recap)

```
App start
    ↓
sessionStore.restore() — if a session exists:
    ↓
pushRepo.registerIfNeeded() — gets current FCM token, POST /notifications/register
    ↓
(token is now bound to user on backend)

Logout
    ↓
pushRepo.unregister() — POST /notifications/unregister
    ↓
SessionStore.clearSession()
```

When FCM rotates the token (rare; happens after app data clear, restore from
backup, etc.), `FcmService.onNewToken(token)` fires. Re-register if a session
is active:

```kotlin
override fun onNewToken(token: String) {
    super.onNewToken(token)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    scope.launch {
        if (sessionStore.isAuthenticated) pushRepo.registerToken(token)
    }
}
```

## Foreground vs background delivery

- **Foreground**: `onMessageReceived` is the *only* place the message lands.
  The system tray notification is not shown automatically — we build it
  ourselves with `NotificationManagerCompat.notify(...)`.
- **Background / killed**: if `notification` block is in the payload, the
  system tray shows it automatically. We still receive a callback into
  `MainActivity.onCreate(intent)` / `onNewIntent(intent)` when the user
  taps it — that's where deep-link parsing happens.

Backend must therefore send BOTH `notification` AND `data` blocks for
correct behavior in all states.

## Testing in dev

Use Firebase Console → Cloud Messaging → "Send test message" with a known
device token. Add a Timber log of the token on first launch to grab it
quickly:

```kotlin
override fun onNewToken(token: String) {
    Timber.d("[push] token: $token")
    // …
}
```

(Strip the log in release builds — never log full FCM tokens in production.)
