package com.arcticfishtrail.game.integration

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.arcticfishtrail.game.MainActivity
import com.arcticfishtrail.game.R

/**
 * Firebase Cloud Messaging entry point: token refresh and incoming pushes.
 */
class PushService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // The token changes on reinstall, restore and clear-data.
        IntegrationStorage.pushToken = token
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val payload = PushPayload.from(message.data)
        showNotification(payload)
    }

    private fun showNotification(payload: PushPayload) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            )
        }

        // The destination url rides in the extras so a cold-start tap carries it, exactly
        // as the system does when it draws the notification itself for a background app.
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_FROM_PUSH, true)
            payload.url?.let { putExtra(PushPayload.KEY_URL, it) }
        }
        val pending = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(payload.title ?: getString(R.string.app_name))
            .setContentText(payload.body.orEmpty())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .build()

        // No-ops without POST_NOTIFICATIONS; the token still arrived, so attribution is
        // unaffected either way.
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val EXTRA_FROM_PUSH = "arctic_from_push"
        private const val CHANNEL_ID = "arctic_offers"
        private const val CHANNEL_NAME = "Updates"
    }
}
