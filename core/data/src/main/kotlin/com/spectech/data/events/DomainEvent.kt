package com.spectech.data.events

/**
 * Cross-feature signals that flow through [AppEventBus]. iOS uses
 * `NotificationCenter` for the same purpose; on Android we use a typed
 * SharedFlow so there's no string-keyed magic and the IDE catches dead
 * subscribers.
 */
sealed interface DomainEvent {
    /** Orders feed changed (create/delete/bid/accept/withdraw). */
    data object OrdersChanged : DomainEvent

    /** Equipment list changed (add/edit/delete/deposit transition). */
    data object EquipmentChanged : DomainEvent

    /** APNs/FCM token arrived; consumers may push it to the backend. */
    data class DeviceTokenReceived(val token: String) : DomainEvent
}
