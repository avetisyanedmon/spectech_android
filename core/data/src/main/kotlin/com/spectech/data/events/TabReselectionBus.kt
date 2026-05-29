package com.spectech.data.events

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Fires when the user taps the bottom-bar tab that's *already* selected.
 *
 * Compose's NavController is per-graph — each tab owns its own nested
 * NavController, and the root NavController doesn't know about the nested
 * back stacks. That makes the iOS-standard "tap the active tab to pop to its
 * root" pattern awkward to implement from the top: there's nothing to pop
 * up there.
 *
 * Instead, [com.spectech.android.navigation.MainTabsScreen] emits the
 * stable tab id (the [TabReselection.id] value) on this bus whenever the
 * current tab is re-tapped. Each per-tab `*NavGraph` subscribes and pops
 * its own back stack down to the start destination when the matching id
 * arrives.
 *
 * Tabs without a nested back stack (e.g. News) can ignore the bus
 * completely — pops are no-ops for them anyway.
 *
 * Stateless, replay = 0: a missed tap is a missed tap. The bus delivers
 * tab-tap intent only; nothing needs to be re-played for late subscribers.
 */
@Singleton
class TabReselectionBus @Inject constructor() {

    private val _tappedTab = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Subscribe in each `*NavGraph` to receive pop-to-root signals. */
    val tappedTab: SharedFlow<String> = _tappedTab.asSharedFlow()

    suspend fun emit(id: String) {
        _tappedTab.emit(id)
    }
}

/**
 * Stable string ids for tab routes. Used as the payload in
 * [TabReselectionBus.tappedTab] so the per-graph subscribers can filter by
 * exact match instead of importing the `:app` `TabRoute` sealed type
 * (feature modules don't depend on `:app`).
 */
object TabReselection {
    const val MARKETPLACE = "tab.marketplace"
    const val MY_BIDS = "tab.my_bids"
    const val MY_ORDERS = "tab.my_orders"
    const val GARAGE = "tab.garage"
    const val NEWS = "tab.news"
}
