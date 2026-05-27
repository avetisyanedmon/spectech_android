package com.spectech.android.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spectech.android.R
import com.spectech.domain.model.User
import com.spectech.uikit.components.EmptyStateView
import com.spectech.uikit.components.SignInPromptView

/**
 * Placeholder Profile/Support/Notifications sheets. Each will be replaced by
 * the corresponding feature in later phases:
 *
 *   ProfileSheet       → Phase 12
 *   SupportSheet       → Phase 14
 *   NotificationsSheet → Phase 11
 *
 * For Phase 4 the only meaningful interaction is logout from ProfileSheet,
 * which keeps the sign-in-then-out smoke test working end to end.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(
    user: User?,
    onSignIn: () -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        if (user == null) {
            SignInPromptView(
                title = stringResource(R.string.profile_title),
                message = stringResource(R.string.signin_prompt_profile),
                actionTitle = stringResource(com.spectech.uikit.R.string.sign_in),
                icon = Icons.Outlined.AccountCircle,
                onSignIn = onSignIn,
            )
        } else {
            ProfilePlaceholder(user = user, onLogout = onLogout)
        }
    }
}

@Composable
private fun ProfilePlaceholder(user: User, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = user.name ?: stringResource(R.string.profile_no_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = user.phone,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.profile_phase_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(50),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.logout),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        EmptyStateView(
            title = stringResource(R.string.support_title),
            message = stringResource(R.string.placeholder_support),
            icon = Icons.Outlined.SupportAgent,
            paddingValues = PaddingValues(0.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    isAuthenticated: Boolean,
    onSignIn: () -> Unit,
    onDismiss: () -> Unit,
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
            EmptyStateView(
                title = stringResource(R.string.notifications_title),
                message = stringResource(R.string.placeholder_notifications),
                icon = Icons.Outlined.Notifications,
                paddingValues = PaddingValues(0.dp),
            )
        }
    }
}
