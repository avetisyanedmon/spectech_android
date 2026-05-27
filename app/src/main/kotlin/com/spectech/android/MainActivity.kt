package com.spectech.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectech.android.navigation.MainTabsScreen
import com.spectech.data.auth.SessionStore
import com.spectech.uikit.components.LoadingStateView
import com.spectech.uikit.theme.SpecTechTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionStore: SessionStore

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { sessionStore.isRestoring.value }
        enableEdgeToEdge()

        setContent {
            SpecTechTheme {
                SpecTechApp(sessionStore = sessionStore)
            }
        }
    }
}

/**
 * Top-level router. Mirrors `RootView` + `RootRouterView` in iOS:
 *   - while the session is being restored from secure storage → loading
 *   - otherwise → [MainTabsScreen] (the auth sheet opens from inside the tabs)
 */
@Composable
private fun SpecTechApp(sessionStore: SessionStore) {
    val isRestoring by sessionStore.isRestoring.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { sessionStore.restore() }

    if (isRestoring) {
        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
            LoadingStateView(
                title = stringResource(R.string.restoring_session),
                paddingValues = padding,
            )
        }
    } else {
        MainTabsScreen(sessionStore = sessionStore)
    }
}
