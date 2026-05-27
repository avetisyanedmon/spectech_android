package com.spectech.domain.state

import com.spectech.domain.error.ApiError

/**
 * Drives every screen that talks to the backend. Mirrors iOS `RemoteState<T>`
 * (SpecTechIOS/Design/RemoteState.swift).
 *
 * ViewModels expose `StateFlow<RemoteState<T>>`; Composables `when`-switch on
 * the cases to render the matching state view from core/ui-kit.
 */
sealed interface RemoteState<out T> {
    data object Idle : RemoteState<Nothing>
    data object Loading : RemoteState<Nothing>
    data class Loaded<T>(val value: T) : RemoteState<T>
    data class Empty(val message: String) : RemoteState<Nothing>
    data class Failed(val error: ApiError) : RemoteState<Nothing>
}
