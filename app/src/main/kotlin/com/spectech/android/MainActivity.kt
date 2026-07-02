package com.spectech.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectech.android.navigation.MainTabsScreen
import com.spectech.data.auth.SessionStore
import com.spectech.data.notifications.NotificationStore
import com.spectech.domain.model.NotificationNavigationRequest
import com.spectech.uikit.theme.SpecTechTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionStore: SessionStore
    @Inject lateinit var notificationStore: NotificationStore

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result handled silently — push still works if denied, the OS just hides the tray notif */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Dismiss the system splash on the first Compose frame so the
        // full-bleed launch artwork in [LaunchSplashView] is what the user
        // sees while the session restore is in flight (matches iOS).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SpecTechTheme {
                SpecTechApp(
                    sessionStore = sessionStore,
                    notificationStore = notificationStore,
                )
            }
        }

        maybeRequestNotificationPermission()
        handleDeepLinkIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // The activity is `launchMode="singleTask"` so notification taps land
        // here while the activity is already running.
        handleDeepLinkIntent(intent)
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        if (data.scheme != "spectech" || data.host != "order") return
        val orderId = data.lastPathSegment.orEmpty()
        if (orderId.isBlank()) return

        notificationStore.requestNavigation(
            NotificationNavigationRequest(
                id = UUID.randomUUID().toString(),
                type = data.getQueryParameter("type"),
                orderId = orderId,
                offerId = data.getQueryParameter("offerId"),
            ),
        )
    }
}

/**
 * Top-level router. Mirrors `RootView` + `RootRouterView` in iOS:
 *   - while the session is being restored from secure storage → full-bleed
 *     launch artwork (same image iOS shows from LaunchScreen.storyboard)
 *   - otherwise → [MainTabsScreen] (the auth sheet opens from inside the tabs)
 */
@Composable
private fun SpecTechApp(
    sessionStore: SessionStore,
    notificationStore: NotificationStore,
) {
    val isRestoring by sessionStore.isRestoring.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { sessionStore.restore() }

    if (isRestoring) {
        LaunchSplashView()
    } else {
        MainTabsScreen(
            sessionStore = sessionStore,
            notificationStore = notificationStore,
        )
    }
}

/**
 * Full-screen launch artwork rendered while the session restore is in flight.
 * Replicates the iOS LaunchScreen storyboard, which paints the same image as a
 * `scaleAspectFill` imageView covering the whole window. We pull the bitmap
 * straight from the iOS asset catalog (`SpecTechIOS/Resources/Assets.xcassets/
 * launch_icon.imageset/wmremove-transformed.png`) so the two platforms ship
 * pixel-identical loading art.
 */
@Composable
private fun LaunchSplashView() {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.launch_image),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}
