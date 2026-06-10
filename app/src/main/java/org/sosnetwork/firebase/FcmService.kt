package org.sosnetwork.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.sosnetwork.app.R
import org.sosnetwork.app.SosApplication.Companion.CHANNEL_SOS
import org.sosnetwork.app.SosApplication.Companion.CHANNEL_FCM

/**
 * Firebase Cloud Messaging Service for handling cross-device notifications.
 * When a user triggers an SOS, they receive push notifications on their other devices
 * through Google's cloud infrastructure.
 */
class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token updated: $token")
        // Store token if needed for server-side push notification targeting
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        val messageTitle = message.notification?.title ?: "SOS Network"
        val messageBody = message.notification?.body ?: ""
        val data = message.data
        
        Log.d(TAG, "FCM Message received: ${message.messageId}")
        Log.d(TAG, "Title: $messageTitle")
        Log.d(TAG, "Body: $messageBody")
        Log.d(TAG, "Data: $data")

        if (isSOSAlert(data)) {
            showSOSNotification(messageTitle, messageBody)
        } else {
            // Handle other types of messages (e.g., test pings, verification requests)
            showGeneralNotification(messageTitle, messageBody)
        }
    }

    private fun isSOSAlert(data: Map<String, String?>): Boolean {
        return data.containsKey("alert_id") && !data["alert_type"]?.equals("ping", true) ?: true
    }

    private fun showSOSNotification(title: String, body: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        
        val notificationIntent = Intent(this, SosAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("title", title)
            putExtra("body", body)
            setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_FCM)
            .setSmallIcon(R.drawable.ic_sos)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))
            .addAction("Acknowledge", "I'm OK")

        notificationManager.notify(NOTIFICATION_ID_SOS, builder.build())
    }

    private fun showGeneralNotification(title: String, body: String) {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_FCM)
            .setSmallIcon(R.drawable.ic_sos)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID_GENERAL, builder.build())
    }

    companion object {
        private const val TAG = "FcmService"
        private const val NOTIFICATION_ID_SOS = 1001
        private const val NOTIFICATION_ID_GENERAL = 1002
    }
}
