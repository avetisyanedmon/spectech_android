package com.spectech.features.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.spectech.features.auth.navigation.AuthRoute
import com.spectech.features.auth.viewmodel.AuthFlowViewModel

/**
 * The auth flow lives in its own nested NavHost so the back stack (start →
 * register → verify-otp) stays isolated from the parent navigator. Mirrors
 * iOS `AuthSheetView`'s embedded `NavigationStack`.
 */
@Composable
fun AuthFlow(
    onSignedIn: () -> Unit,
    viewModel: AuthFlowViewModel = hiltViewModel(),
) {
    val nav = rememberNavController()
    val pendingPhone by viewModel.pendingVerifyPhone.collectAsStateWithLifecycle()

    LaunchedEffect(pendingPhone) {
        val phone = pendingPhone ?: return@LaunchedEffect
        nav.navigate(AuthRoute.VerifyOtp(phone))
        viewModel.consumePendingVerifyPhone()
    }

    NavHost(nav, startDestination = AuthRoute.Start) {
        composable<AuthRoute.Start> {
            StartAuthScreen(
                viewModel = viewModel,
                onCreateAccount = { nav.navigate(AuthRoute.Register) },
            )
        }
        composable<AuthRoute.Register> {
            RegisterScreen(
                viewModel = viewModel,
                onSignIn = { nav.popBackStack(AuthRoute.Start, inclusive = false) },
            )
        }
        composable<AuthRoute.VerifyOtp> { backStackEntry ->
            val args = backStackEntry.toRoute<AuthRoute.VerifyOtp>()
            VerifyOtpScreen(viewModel = viewModel, phone = args.phone)
        }
    }
}
