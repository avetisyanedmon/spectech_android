package com.spectech.features.auth.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectech.data.auth.AuthRepository
import com.spectech.domain.error.ApiError
import com.spectech.domain.model.RegistrationProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the OTP flow. Direct port of iOS `AuthFlowViewModel`
 * (SpecTechIOS/Features/Auth/AuthFlow.swift).
 *
 * `pendingVerifyPhone` is exposed as a StateFlow so the auth navigation host
 * can observe it and push the verify screen automatically once the OTP is on
 * its way.
 */
@HiltViewModel
class AuthFlowViewModel @Inject constructor(
    private val authRepo: AuthRepository,
) : ViewModel() {

    var startError by mutableStateOf<ApiError?>(null); private set
    var verifyError by mutableStateOf<ApiError?>(null); private set
    var isSubmittingPhone by mutableStateOf(false); private set
    var isVerifyingCode by mutableStateOf(false); private set

    /** Profile collected on the register screen; null for sign-in. */
    var registrationProfile: RegistrationProfile? = null

    private val _pendingVerifyPhone = MutableStateFlow<String?>(null)
    val pendingVerifyPhone: StateFlow<String?> = _pendingVerifyPhone.asStateFlow()

    fun submitPhone(phone: String) {
        viewModelScope.launch {
            isSubmittingPhone = true
            startError = null
            runCatching { authRepo.startAuth(phone) }
                .onSuccess { _pendingVerifyPhone.value = it }
                .onFailure { startError = ApiError.from(it) }
            isSubmittingPhone = false
        }
    }

    fun submitCode(phone: String, code: String) {
        viewModelScope.launch {
            isVerifyingCode = true
            verifyError = null
            runCatching { authRepo.verifyAuth(phone, code, registrationProfile) }
                .onFailure { verifyError = ApiError.from(it) }
            isVerifyingCode = false
        }
    }

    fun consumePendingVerifyPhone() {
        _pendingVerifyPhone.value = null
    }
}
