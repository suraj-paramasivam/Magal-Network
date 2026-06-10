package org.sosnetwork.app.mesh

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.sosnetwork.app.R
import org.sosnetwork.app.SosApplication
import org.sosnetwork.app.ui.SosAlertActivity
import org.sosnetwork.protocol.SosAlertPayload

object SosAlertNotifier {
    fun show(context: Context, payload: SosAlertPayload, voicePath: String?) {
        val intent = buildAlertIntent(context, payload, voicePath)
        val pending = PendingIntent.getActivity(
            context,
            payload.alertId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, SosApplication.CHANNEL_SOS)
            .setSmallIcon(R.drawable.ic_sos)
            .setContentTitle(context.getString(R.string.incoming_sos_title))
            .setContentText(
                payload.senderDisplayName ?: context.getString(R.string.unknown_sender),
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setFullScreenIntent(pending, true)
            .build()

        context.getSystemService(android.app.NotificationManager::class.java)
            .notify(payload.alertId.hashCode(), notification)
        context.startActivity(intent)
    }

    fun showAck(context: Context, alertId: String, responderName: String, message: String) {
        val notification = NotificationCompat.Builder(context, SosApplication.CHANNEL_SOS)
            .setSmallIcon(R.drawable.ic_sos)
            .setContentTitle(context.getString(R.string.sos_ack_title, responderName))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        context.getSystemService(android.app.NotificationManager::class.java)
            .notify("ack-$alertId".hashCode(), notification)
    }

    private fun buildAlertIntent(
        context: Context,
        payload: SosAlertPayload,
        voicePath: String?,
    ): Intent =
        Intent(context, SosAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(SosAlertActivity.EXTRA_ALERT_ID, payload.alertId)
            putExtra(SosAlertActivity.EXTRA_SENDER, payload.senderDisplayName)
            putExtra(SosAlertActivity.EXTRA_LAT, payload.location.latitude)
            putExtra(SosAlertActivity.EXTRA_LNG, payload.location.longitude)
            putExtra(SosAlertActivity.EXTRA_MESSAGE, payload.messageText)
            putExtra(SosAlertActivity.EXTRA_VOICE_PATH, voicePath)
            putExtra(SosAlertActivity.EXTRA_VERIFICATION, payload.senderVerificationLevel.name)
        }
}
