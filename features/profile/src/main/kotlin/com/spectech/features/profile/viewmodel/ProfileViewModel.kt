package com.spectech.features.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectech.data.auth.SessionStore
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
     * Signs the user out. The push token forget is best-effort and runs
     * before the session is cleared so the bearer is still present for any
     * future server-side unregister call we might add. The session clear
     * triggers the SpecTechApplication push-registration collector, which
     * also calls `pushRepository.forget()` — that's idempotent.
     */
    fun logout() {
        viewModelScope.launch {
            pushRepository.forget()
            // Drop the local saved-filter mirror so the next user that signs
            // in on this device doesn't see the previous account's picks
            // while [SavedFilterStore.loadFromServer] catches up.
            savedFilterStore.forgetLocal()
            sessionStore.clearSession()
            profileStore.clear()
        }
    }
}
