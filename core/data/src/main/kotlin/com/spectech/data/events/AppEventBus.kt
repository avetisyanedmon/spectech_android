package com.spectech.data.events

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-wide event bus. Repositories `emit` after mutating server state;
 * ViewModels collect to refresh their own lists.
 *
 * Single-instance Hilt singleton — events are not replayed to new
 * subscribers, but the buffer ensures a brief burst (e.g. accept + emit
 * during one user action) doesn't drop signals if collectors are slow.
 */
@Singleton
class AppEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<DomainEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<DomainEvent> = _events.asSharedFlow()

    suspend fun emit(event: DomainEvent) {
        _events.emit(event)
    }
}
