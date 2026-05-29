package com.spectech.android.push

import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

/**
 * Wraps Firebase's `Task<String>`-based token API in a suspend function that
 * fails soft: returns null when Firebase isn't initialized, when Google Play
 * services are unavailable, or when the task is cancelled. Mirrors the
 * "best-effort" semantics iOS gets from `UNUserNotificationCenter`.
 *
 * Lives in `:app` so neither `:core:data` nor `:core:platform` needs a
 * Firebase dependency.
 */
object PushTokenSource {

    /**
     * Returns the current FCM token, or null if it can't be resolved (no
     * Firebase project wired, no Google Play services, …).
     */
    suspend fun current(): String? = suspendCancellableCoroutine { cont ->
        val task = runCatching { FirebaseMessaging.getInstance().token }.getOrElse {
            Timber.tag(TAG).w(it, "Firebase not initialized; skipping token fetch")
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        task
            .addOnSuccessListener { token -> cont.resume(token) }
            .addOnFailureListener { e ->
                Timber.tag(TAG).w(e, "FCM token fetch failed")
                cont.resume(null)
            }
            .addOnCanceledListener { cont.resume(null) }
    }

    private const val TAG = "PushTokenSource"
}
