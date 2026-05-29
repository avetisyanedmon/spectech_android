package com.spectech.features.profile.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectech.domain.model.LocalProfile
import com.spectech.domain.model.User
import com.spectech.features.profile.R
import com.spectech.features.profile.viewmodel.ProfileViewModel
import com.spectech.uikit.components.SignInPromptView

/**
 * Profile tab content. Mirrors iOS `ProfileView` — two modes:
 *   - not signed in → big sign-in CTA + privacy/terms footer
 *   - signed in → profile card + menu + logout
 *
 * Edit profile, language picker, and notifications all open via [onOpenEdit]
 * / [onOpenLanguage] / [onOpenNotifications] so the host (the modal sheet in
 * `:app`) can decide whether to navigate, push a nested sheet, or wire any of
 * those into existing flows. Keeping that choice outside this feature
 * preserves the "features don't depend on each other" rule.
 */
@Composable
fun ProfileScreen(
    versionName: String,
    unreadNotificationCount: Int,
    onSignIn: () -> Unit,
    onOpenEdit: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val session by viewModel.sessionFlow.collectAsStateWithLifecycle()
    val localProfile by viewModel.profileFlow.collectAsStateWithLifecycle()
    val user = session?.user

    if (user == null) {
        SignedOutBody(
            onSignIn = onSignIn,
            modifier = modifier,
        )
    } else {
        SignedInBody(
            user = user,
            localProfile = localProfile,
            versionName = versionName,
            unreadNotificationCount = unreadNotificationCount,
            onOpenEdit = onOpenEdit,
            onOpenLanguage = onOpenLanguage,
            onOpenNotifications = onOpenNotifications,
            onLogoutConfirmed = {
                viewModel.logout()
                onLoggedOut()
            },
            profileViewModel = viewModel,
            modifier = modifier,
        )
    }
}

@Composable
private fun SignedOutBody(
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SignInPromptView(
            title = stringResource(R.string.profile_title),
            message = stringResource(R.string.profile_signin_message),
            actionTitle = stringResource(com.spectech.uikit.R.string.sign_in),
            icon = Icons.Outlined.AccountCircle,
            onSignIn = onSignIn,
            modifier = Modifier.weight(1f),
        )
        LegalLinksFooter(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        )
    }
}

@Composable
private fun SignedInBody(
    user: User,
    localProfile: LocalProfile,
    versionName: String,
    unreadNotificationCount: Int,
    onOpenEdit: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onLogoutConfirmed: () -> Unit,
    profileViewModel: ProfileViewModel,
    modifier: Modifier = Modifier,
) {
    var showLogoutConfirm by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProfileCard(user = user, localProfile = localProfile)
        MenuCard(
            unreadNotificationCount = unreadNotificationCount,
            onEdit = onOpenEdit,
            onLanguage = onOpenLanguage,
            onNotifications = onOpenNotifications,
        )
        SavedFilterRow(viewModel = profileViewModel)
        LogoutButton(onClick = { showLogoutConfirm = true })
        VersionFooter(versionName = versionName)
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(stringResource(R.string.profile_logout_confirm_title)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    onLogoutConfirmed()
                }) {
                    Text(
                        text = stringResource(R.string.profile_logout_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(stringResource(R.string.profile_logout_cancel))
                }
            },
        )
    }
}

/**
 * Reads the user's display name with a three-step fallback that matches iOS'
 * `ProfileView.displayName` (SpecTechIOS/Features/Profile/ProfileView.swift:260-264):
 *
 *   1. server-side [User.name] when populated
 *   2. locally-cached [LocalProfile.displayName] when populated
 *   3. the localized "User" placeholder
 *
 * Same shape for [resolveDisplayEmail] — server first, local cache second,
 * "—" placeholder last.
 */
@Composable
private fun resolveDisplayName(user: User, localProfile: LocalProfile): String {
    val fromUser = user.name?.takeIf { it.isNotBlank() }
    if (fromUser != null) return fromUser
    val fromCache = localProfile.displayName.takeIf { it.isNotBlank() }
    if (fromCache != null) return fromCache
    return stringResource(R.string.profile_default_user)
}

@Composable
private fun resolveDisplayEmail(user: User, localProfile: LocalProfile): String {
    val fromUser = user.email?.takeIf { it.isNotBlank() }
    if (fromUser != null) return fromUser
    val fromCache = localProfile.email.takeIf { it.isNotBlank() }
    if (fromCache != null) return fromCache
    return stringResource(R.string.profile_placeholder_dash)
}

@Composable
private fun ProfileCard(user: User, localProfile: LocalProfile) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resolveDisplayName(user, localProfile),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.profile_unified_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        InfoRow(label = stringResource(R.string.profile_field_email), value = resolveDisplayEmail(user, localProfile))
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        InfoRow(label = stringResource(R.string.profile_field_phone), value = formatRussianPhone(user.phone))
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        InfoRow(label = stringResource(R.string.profile_field_city), value = user.city.dashIfBlank())
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun MenuCard(
    unreadNotificationCount: Int,
    onEdit: () -> Unit,
    onLanguage: () -> Unit,
    onNotifications: () -> Unit,
) {
    val ctx = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        MenuRow(title = stringResource(R.string.profile_menu_edit), onClick = onEdit)
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        MenuRow(title = stringResource(R.string.profile_menu_language), onClick = onLanguage)
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        NotificationsMenuRow(
            unreadCount = unreadNotificationCount,
            onClick = onNotifications,
        )
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        MenuRow(
            title = stringResource(R.string.profile_menu_privacy),
            onClick = { openUrl(ctx, PRIVACY_URL) },
        )
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        MenuRow(
            title = stringResource(R.string.profile_menu_terms),
            onClick = { openUrl(ctx, TERMS_URL) },
        )
    }
}

@Composable
private fun MenuRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NotificationsMenuRow(unreadCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.profile_menu_notifications),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.weight(1f))
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.error, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
            Spacer(Modifier.size(8.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
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
            text = stringResource(R.string.profile_logout),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun VersionFooter(versionName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.profile_version, versionName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LegalLinksFooter(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = { openUrl(ctx, PRIVACY_URL) }) {
            Text(stringResource(R.string.profile_menu_privacy))
        }
        TextButton(onClick = { openUrl(ctx, TERMS_URL) }) {
            Text(stringResource(R.string.profile_menu_terms))
        }
    }
}

private fun openUrl(ctx: android.content.Context, url: String) {
    runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

@Composable
private fun String?.dashIfBlank(): String {
    val placeholder = stringResource(R.string.profile_placeholder_dash)
    return remember(this, placeholder) {
        if (this.isNullOrBlank()) placeholder else this
    }
}

/**
 * Russian-format `+7 (XXX) XXX-XX-XX` for an 11-digit phone starting with 7.
 * Falls back to the raw value for anything that doesn't match — the formatter
 * is purely cosmetic, the backend stores E.164.
 */
private fun formatRussianPhone(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    if (digits.length != 11 || !digits.startsWith('7')) return phone
    val rest = digits.drop(1)
    return "+7 (${rest.substring(0, 3)}) ${rest.substring(3, 6)}-${rest.substring(6, 8)}-${rest.substring(8, 10)}"
}

private const val PRIVACY_URL = "https://www.spectechweb.ru/privacy"
private const val TERMS_URL = "https://www.spectechweb.ru/terms"

// Padding-values overload for hosts that want to consume insets themselves.
@Composable
fun ProfileScreen(
    paddingValues: PaddingValues,
    versionName: String,
    unreadNotificationCount: Int,
    onSignIn: () -> Unit,
    onOpenEdit: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    ProfileScreen(
        versionName = versionName,
        unreadNotificationCount = unreadNotificationCount,
        onSignIn = onSignIn,
        onOpenEdit = onOpenEdit,
        onOpenLanguage = onOpenLanguage,
        onOpenNotifications = onOpenNotifications,
        onLoggedOut = onLoggedOut,
        modifier = Modifier.padding(paddingValues),
    )
}
