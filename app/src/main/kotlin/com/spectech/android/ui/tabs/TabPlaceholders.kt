package com.spectech.android.ui.tabs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.ContentPasteSearch
import androidx.compose.material.icons.outlined.Garage
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.spectech.android.R
import com.spectech.uikit.components.EmptyStateView
import com.spectech.uikit.components.SignInPromptView

/**
 * Tab content stubs for auth-required tabs that haven't been built out. Each
 * shows a [SignInPromptView] when anonymous and a plain [EmptyStateView]
 * when signed in (until the feature replaces the stub via its NavHost).
 */
@Composable
fun MyBidsTabPlaceholder(
    isAuthenticated: Boolean,
    onSignIn: () -> Unit,
    padding: PaddingValues,
) {
    if (!isAuthenticated) {
        SignInPromptView(
            title = stringResource(R.string.tab_title_my_bids),
            message = stringResource(R.string.signin_prompt_my_bids),
            actionTitle = stringResource(com.spectech.uikit.R.string.sign_in),
            icon = Icons.Outlined.AssignmentTurnedIn,
            onSignIn = onSignIn,
            paddingValues = padding,
        )
    } else {
        EmptyStateView(
            title = stringResource(R.string.tab_title_my_bids),
            message = stringResource(R.string.placeholder_my_bids),
            icon = Icons.Outlined.AssignmentTurnedIn,
            paddingValues = padding,
        )
    }
}

@Composable
fun MyOrdersTabPlaceholder(
    isAuthenticated: Boolean,
    onSignIn: () -> Unit,
    padding: PaddingValues,
) {
    if (!isAuthenticated) {
        SignInPromptView(
            title = stringResource(R.string.tab_title_my_orders),
            message = stringResource(R.string.signin_prompt_my_orders),
            actionTitle = stringResource(com.spectech.uikit.R.string.sign_in),
            icon = Icons.Outlined.ContentPasteSearch,
            onSignIn = onSignIn,
            paddingValues = padding,
        )
    } else {
        EmptyStateView(
            title = stringResource(R.string.tab_title_my_orders),
            message = stringResource(R.string.placeholder_my_orders),
            icon = Icons.Outlined.ContentPasteSearch,
            paddingValues = padding,
        )
    }
}

@Composable
fun GarageTabPlaceholder(
    isAuthenticated: Boolean,
    onSignIn: () -> Unit,
    padding: PaddingValues,
) {
    if (!isAuthenticated) {
        SignInPromptView(
            title = stringResource(R.string.tab_title_garage),
            message = stringResource(R.string.signin_prompt_garage),
            actionTitle = stringResource(com.spectech.uikit.R.string.sign_in),
            icon = Icons.Outlined.Garage,
            onSignIn = onSignIn,
            paddingValues = padding,
        )
    } else {
        EmptyStateView(
            title = stringResource(R.string.tab_title_garage),
            message = stringResource(R.string.placeholder_garage),
            icon = Icons.Outlined.Garage,
            paddingValues = padding,
        )
    }
}
