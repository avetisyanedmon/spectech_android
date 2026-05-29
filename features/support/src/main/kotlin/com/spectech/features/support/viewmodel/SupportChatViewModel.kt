package com.spectech.features.support.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectech.data.auth.SessionStore
import com.spectech.data.support.SupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Backs the "Contact support" sheet. Single-flight submission via
 * [isSending] and a sticky [sentSuccessfully] flag so the success state
 * survives a recomposition before the sheet auto-dismisses.
 */
@HiltViewModel
class SupportChatViewModel @Inject constructor(
    private val supportRepository: SupportRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    var messageText by mutableStateOf("")
        private set
    var orderIdText by mutableStateOf("")
        private set
    var isSending by mutableStateOf(false)
        private set
    var sentSuccessfully by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val canSend: Boolean
        get() = messageText.trim().isNotEmpty() && !isSending

    fun onMessageChange(value: String) {
        messageText = value
        if (errorMessage != null) errorMessage = null
    }

    fun onOrderIdChange(value: String) {
        orderIdText = value
        if (errorMessage != null) errorMessage = null
    }

    fun send(onErrorFallback: String) {
        val trimmed = messageText.trim()
        if (trimmed.isEmpty() || isSending) return
        isSending = true
        errorMessage = null
        viewModelScope.launch {
            val orderId = orderIdText.trim().takeIf { it.isNotEmpty() }
            runCatching {
                supportRepository.sendMessage(
                    message = trimmed,
                    contactInfo = sessionStore.currentUser?.phone,
                    relatedEntityType = orderId?.let { "order" },
                    relatedEntityId = orderId,
                )
            }
                .onSuccess {
                    sentSuccessfully = true
                    messageText = ""
                    orderIdText = ""
                }
                .onFailure { errorMessage = onErrorFallback }
            isSending = false
        }
    }
}
