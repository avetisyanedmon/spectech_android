package com.spectech.android.ui.sheets

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.spectech.android.R
import com.spectech.domain.model.AppNotification
import com.spectech.features.notifications.ui.NotificationsScreen
import com.spectech.uikit.components.SignInPromptView

/**
 * Bottom-sheet hosts for sibling overlays driven by [MainTabsScreen].
 *
 * Profile UI lives in `:features:profile.ProfileScreen`, Notifications UI lives
 * in `:features:notifications.NotificationsScreen`, Support lives in
 * `:features:support.SupportChatSheet` — what's left here is the
 * `ModalBottomSheet` scaffolding plus pre-auth gating so each feature can stay
 * a pure stateless composable that receives a snapshot + callbacks.
 *
 * Sheets are kept mutually exclusive at this layer because Compose's nested
 * `ModalBottomSheet` support is fragile on Android.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(
    versionName: String,
    unreadNotificationCount: Int,
    onSignIn: () -> Unit,
    onOpenEdit: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        com.spectech.features.profile.ui.ProfileScreen(
            versionName = versionName,
            unreadNotificationCount = unreadNotificationCount,
            onSignIn = onSignIn,
            onOpenEdit = onOpenEdit,
            onOpenLanguage = onOpenLanguage,
            onOpenNotifications = onOpenNotifications,
            onLoggedOut = onDismiss,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    isAuthenticated: Boolean,
    notifications: List<AppNotification>,
    onSignIn: () -> Unit,
    onDismiss: () -> Unit,
    onMarkAllRead: () -> Unit,
    onClearAll: () -> Unit,
    onMarkRead: (AppNotification) -> Unit,
    onTap: (AppNotification) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        if (!isAuthenticated) {
            SignInPromptView(
                title = stringResource(R.string.notifications_title),
                message = stringResource(R.string.signin_prompt_notifications),
                actionTitle = stringResource(com.spectech.uikit.R.string.sign_in),
                icon = Icons.Outlined.Notifications,
                onSignIn = onSignIn,
            )
        } else {
            NotificationsScreen(
                notifications = notifications,
                onTap = onTap,
                onMarkRead = onMarkRead,
                onMarkAllRead = onMarkAllRead,
                onClearAll = onClearAll,
            )
        }
    }
}
