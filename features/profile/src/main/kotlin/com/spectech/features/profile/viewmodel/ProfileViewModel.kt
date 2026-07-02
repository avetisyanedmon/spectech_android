package com.spectech.features.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectech.data.auth.SessionStore
import com.spectech.data.notifications.NotificationStore
import com.spectech.data.profile.ProfileStore
import com.spectech.data.push.PushRepository
import com.spectech.data.savedfilter.SavedFilterStore
import com.spectech.domain.enums.AppLanguage
import com.spectech.features.profile.language.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Backs `ProfileScreen`. Owns the language-pick + notifications-toggle
 * persistence and the logout action. Edit-profile lives in its own VM since
 * it's a separate destination with its own load/submit state machine.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val profileStore: ProfileStore,
    private val pushRepository: PushRepository,
    private val savedFilterStore: SavedFilterStore,
    private val notificationStore: NotificationStore,
) : ViewModel() {

    val sessionFlow = sessionStore.currentSession
    val profileFlow = profileStore.profile
    val savedFilterFlow = savedFilterStore.savedFilter
    val savedFilterNotificationsEnabledFlow = savedFilterStore.notificationsEnabled
    val savedFilterSyncingFlow = savedFilterStore.isSyncing

    /** Persists the pick and applies it via per-app locale immediately. */
    fun pickLanguage(language: AppLanguage) {
        LocaleManager.apply(language)
        viewModelScope.launch {
            profileStore.update { it.copy(language = language) }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            profileStore.update { it.copy(notificationsEnabled = enabled) }
        }
    }

    /**
     * Mirror of the marketplace sheet's notify-on-matching-orders toggle.
     * Caller is responsible for the runtime POST_NOTIFICATIONS permission
     * when [enabled] is true (the row composable handles the launcher).
     */
    fun setSavedFilterNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { savedFilterStore.setNotificationsEnabled(enabled) }
    }

    /**
     * Signs the user out. The server-side push unregister is best-effort and
     * MUST run before the session is cleared — it needs the bearer token, and
     * skipping it would leave this device receiving the old account's
     * notifications. The session clear then triggers the SpecTechApplication
     * push-registration collector, which calls `pushRepository.forget()` —
     * that's idempotent with the unregister.
     */
    fun logout() {
        viewModelScope.launch {
            pushRepository.unregisterLastToken()
            // Drop the local saved-filter mirror so the next user that signs
            // in on this device doesn't see the previous account's picks
            // while [SavedFilterStore.loadFromServer] catches up.
            savedFilterStore.forgetLocal()
            // The persisted in-app inbox belongs to the account, not the
            // device — wipe it so the next sign-in doesn't see the previous
            // user's bid/order notifications.
            notificationStore.clear()
            sessionStore.clearSession()
            profileStore.clear()
        }
    }
}
