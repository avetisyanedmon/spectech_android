package com.spectech.features.profile.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectech.data.auth.SessionStore
import com.spectech.data.profile.ProfileRepository
import com.spectech.domain.error.ApiError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Backs `EditProfileScreen`. Form state is held as Compose `mutableStateOf`
 * to keep the form ergonomic without forcing the screen to manage its own
 * scaffolding StateFlows. Submit is single-flight via [isSubmitting].
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    sessionStore: SessionStore,
) : ViewModel() {

    var name by mutableStateOf(sessionStore.currentUser?.name.orEmpty())
        private set
    var email by mutableStateOf(sessionStore.currentUser?.email.orEmpty())
        private set
    var city by mutableStateOf(sessionStore.currentUser?.city.orEmpty())
        private set

    var isSubmitting by mutableStateOf(false)
        private set
    var error by mutableStateOf<ApiError?>(null)
        private set

    fun onNameChange(value: String) { name = value; error = null }
    fun onEmailChange(value: String) { email = value; error = null }
    fun onCityChange(value: String) { city = value; error = null }

    /**
     * Persists the edit. Calls [onSuccess] only after the session is updated
     * so the caller can safely dismiss the sheet — by then the profile
     * screen behind it will already read the new user fields.
     */
    fun save(onSuccess: () -> Unit) {
        if (isSubmitting) return
        isSubmitting = true
        error = null
        viewModelScope.launch {
            runCatching { profileRepository.updateProfile(name, email, city) }
                .onSuccess { onSuccess() }
                .onFailure { error = ApiError.from(it) }
            isSubmitting = false
        }
    }
}
