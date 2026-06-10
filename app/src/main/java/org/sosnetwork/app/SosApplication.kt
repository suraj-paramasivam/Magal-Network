package org.sosnetwork.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import org.sosnetwork.app.data.AppContainer

class SosApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase - must be before creating context-aware objects
        FirebaseApp.initializeApp(this)
        
        container = AppContainer(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        listOf(
            CHANNEL_SOS to "Emergency SOS Alerts",
            CHANNEL_MESH to "SOS Network Mesh",
            CHANNEL_FCM to "Push Notifications (FCM)",
        ).forEach { (id, name) ->
            try {
                manager.createNotificationChannel(
                    NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                        description = if (id == CHANNEL_FCM) "Cloud-based push notifications for cross-device alerts" else "SOS Network decentralized alerts"
                        enableVibration(true)
                        setShowBadge(true)
                    }
                )
            } catch (e: Exception) {
                // Channel may already exist
            }
        }
    }

    companion object {
        const val CHANNEL_SOS = "sos_alerts"
        const val CHANNEL_MESH = "sos_mesh"
        const val CHANNEL_FCM = "fcm_notifications"
        
        // Firebase Messaging instance for background operations
        fun getMessaging(): FirebaseMessaging? = 
            try {
                FirebaseMessaging.getInstance()
            } catch (e: Exception) {
                null
            }
    }
}
