package com.spectech.android.push

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.spectech.android.MainActivity
import com.spectech.android.R
import com.spectech.android.SpecTechApplication
import com.spectech.data.auth.SessionStore
import com.spectech.data.notifications.NotificationStore
import com.spectech.data.push.PushRepository
import com.spectech.domain.model.AppNotification
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import timber.log.Timber

/**
 * Listens for FCM messages. Mirrors iOS `AppDelegate` push handling:
 *   - `onNewToken` → forward to backend (if signed in)
 *   - `onMessageReceived` → push the parsed [AppNotification] into the
 *     in-app inbox AND raise a system-tray notification with a deep-link
 *     PendingIntent so the user can route into the relevant order detail.
 *
 * Inert until Firebase is initialized via `google-services.json` + the
 * Google Services plugin in `app/build.gradle.kts`.
 */
@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject lateinit var notificationStore: NotificationStore
    @Inject lateinit var pushRepository: PushRepository
    @Inject lateinit var sessionStore: SessionStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.tag(TAG).d("FCM onNewToken")
        scope.launch {
            if (sessionStore.isAuthenticated) pushRepository.registerToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val parsed = parse(message) ?: return
        notificationStore.add(parsed)
        showSystemNotification(parsed)
    }

    private fun parse(message: RemoteMessage): AppNotification? {
        val data = message.data
        val notification = message.notification
        val title = notification?.title ?: data["title"] ?: return null
        val body = notification?.body ?: data["body"] ?: return null
        return AppNotification(
            id = UUID.randomUUID().toString(),
            notificationId = data["notificationId"],
            title = title,
            body = body,
            type = data["type"],
            orderId = data["orderId"],
            offerId = data["offerId"],
            bidId = data["bidId"] ?: data["offerId"],
            receivedAt = Clock.System.now(),
            isRead = false,
        )
    }

    private fun showSystemNotification(n: AppNotification) {
        val mgr = NotificationManagerCompat.from(this)
        if (!mgr.areNotificationsEnabled()) return  // User revoked POST_NOTIFICATIONS.

        val pendingIntent = buildPendingIntent(n)
        val builder = NotificationCompat.Builder(this, SpecTechApplication.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(n.title)
            .setContentText(n.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(n.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        try {
            mgr.notify(n.id.hashCode(), builder.build())
        } catch (e: SecurityException) {
            Timber.tag(TAG).w(e, "POST_NOTIFICATIONS permission revoked; dropping system notification")
        }
    }

    private fun buildPendingIntent(n: AppNotification): PendingIntent {
        // Deep link the activity so the navigator can react via onNewIntent.
        val orderId = n.orderId.orEmpty()
        val uri = buildString {
            append("spectech://order/")
            append(orderId)
            val params = listOfNotNull(
                n.type?.takeIf { it.isNotEmpty() }?.let { "type=$it" },
                n.offerId?.takeIf { it.isNotEmpty() }?.let { "offerId=$it" },
            )
            if (params.isNotEmpty()) append('?').append(params.joinToString("&"))
        }.toUri()

        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = uri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            n.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val TAG = "FcmService"
    }
}
