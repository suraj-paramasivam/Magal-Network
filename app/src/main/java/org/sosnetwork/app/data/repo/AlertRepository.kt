package org.sosnetwork.app.data.repo

import kotlinx.coroutines.flow.Flow
import org.sosnetwork.app.data.local.AlertDao
import org.sosnetwork.app.data.local.ReceivedAlertEntity
import org.sosnetwork.protocol.SosAlertPayload
import org.sosnetwork.protocol.VerificationLevel

class AlertRepository(private val alertDao: AlertDao) {
    fun observeAlerts(): Flow<List<ReceivedAlertEntity>> = alertDao.observeAlerts()

    suspend fun hasSeenMessage(messageId: String): Boolean = alertDao.hasSeenMessage(messageId)

    suspend fun markSeen(messageId: String) {
        alertDao.markSeen(messageId, System.currentTimeMillis())
    }

    suspend fun saveIncomingAlert(payload: SosAlertPayload, senderPeerId: String, voicePath: String?) {
        alertDao.insert(
            ReceivedAlertEntity(
                alertId = payload.alertId,
                senderPeerId = senderPeerId,
                senderDisplayName = payload.senderDisplayName,
                latitude = payload.location.latitude,
                longitude = payload.location.longitude,
                accuracyMeters = payload.location.accuracyMeters,
                messageText = payload.messageText,
                voiceNotePath = voicePath,
                severity = payload.severity.name,
                verificationLevel = payload.senderVerificationLevel.name,
                receivedAtEpochMs = System.currentTimeMillis(),
                acknowledged = false,
            )
        )
    }

    suspend fun acknowledge(alertId: String) = alertDao.acknowledge(alertId)

    suspend fun getAlert(alertId: String): ReceivedAlertEntity? = alertDao.getByAlertId(alertId)
}
