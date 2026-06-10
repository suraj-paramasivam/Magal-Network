package org.sosnetwork.app.mesh

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.sosnetwork.app.data.local.VerificationRequestEntity
import org.sosnetwork.app.data.repo.AlertRepository
import org.sosnetwork.app.data.repo.IdentityRepository
import org.sosnetwork.app.data.repo.VerificationRepository
import org.sosnetwork.protocol.AdminAttestation
import org.sosnetwork.protocol.AttestationType
import org.sosnetwork.protocol.AttestationValidator
import org.sosnetwork.protocol.GeoLocation
import org.sosnetwork.protocol.GovernmentIdType
import org.sosnetwork.protocol.IdentityAnnouncePayload
import org.sosnetwork.protocol.MessageType
import org.sosnetwork.protocol.SosAckPayload
import org.sosnetwork.protocol.SosAlertPayload
import org.sosnetwork.protocol.SosEnvelope
import org.sosnetwork.protocol.SosProtocol
import org.sosnetwork.protocol.VerificationLevel
import org.sosnetwork.protocol.VerificationRequestPayload
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

class SosMeshCoordinator(
    private val context: Context,
    private val identityRepository: IdentityRepository,
    private val alertRepository: AlertRepository,
    private val verificationRepository: VerificationRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _incomingAlerts = MutableSharedFlow<SosAlertPayload>(extraBufferCapacity = 16)
    val incomingAlerts: SharedFlow<SosAlertPayload> = _incomingAlerts
    private val pendingOutbound = ConcurrentLinkedQueue<ByteArray>()

    var meshService: SosMeshService? = null

    fun startMesh() {
        context.startForegroundService(Intent(context, SosMeshService::class.java))
    }

    fun stopMesh() {
        context.stopService(Intent(context, SosMeshService::class.java))
    }

    fun onMeshStarted() {
        scope.launch {
            announceIdentity()
            flushPendingOutbound()
        }
    }

    suspend fun broadcastSos(
        location: GeoLocation,
        messageText: String?,
        voiceBytes: ByteArray?,
        voiceMime: String = "audio/mp4",
    ): String {
        val identity = identityRepository.getIdentity()
            ?: error("Register before sending SOS")
        val alertId = UUID.randomUUID().toString()
        val level = runCatching {
            VerificationLevel.valueOf(identity.verificationLevel)
        }.getOrDefault(VerificationLevel.UNVERIFIED)

        val cappedVoice = voiceBytes?.let { capVoicePayload(it) }

        val payload = SosAlertPayload(
            alertId = alertId,
            location = location,
            messageText = messageText?.takeIf { it.isNotBlank() },
            voiceNoteMime = cappedVoice?.let { voiceMime },
            voiceNoteBase64 = cappedVoice?.let { Base64.getEncoder().encodeToString(it) },
            senderDisplayName = identity.displayName,
            senderVerificationLevel = level,
        )

        val envelope = SosEnvelope(
            messageId = UUID.randomUUID().toString(),
            type = MessageType.SOS_ALERT,
            senderPeerId = identity.peerId,
            timestampEpochMs = System.currentTimeMillis(),
            hopCount = 0,
            payload = SosProtocol.json.encodeToString(payload),
        )

        sendBytes(SosProtocol.encodeEnvelope(envelope))
        return alertId
    }

    suspend fun sendSosAck(alertId: String, message: String? = null) {
        val identity = identityRepository.getIdentity() ?: return
        val payload = SosAckPayload(
            alertId = alertId,
            responderPeerId = identity.peerId,
            responderDisplayName = identity.displayName,
            message = message,
        )
        val envelope = SosEnvelope(
            messageId = UUID.randomUUID().toString(),
            type = MessageType.SOS_ACK,
            senderPeerId = identity.peerId,
            timestampEpochMs = System.currentTimeMillis(),
            payload = SosProtocol.json.encodeToString(payload),
        )
        sendBytes(SosProtocol.encodeEnvelope(envelope))
        alertRepository.acknowledge(alertId)
    }

    suspend fun announceVerificationRequest(
        peerId: String,
        displayName: String,
        governmentIdHash: String,
        idType: GovernmentIdType,
    ) {
        val payload = VerificationRequestPayload(
            peerId = peerId,
            displayName = displayName,
            governmentIdHash = governmentIdHash,
            idDocumentType = idType,
            submittedAtEpochMs = System.currentTimeMillis(),
        )
        val envelope = SosEnvelope(
            messageId = UUID.randomUUID().toString(),
            type = MessageType.VERIFICATION_REQUEST,
            senderPeerId = peerId,
            timestampEpochMs = System.currentTimeMillis(),
            payload = SosProtocol.json.encodeToString(payload),
        )
        sendBytes(SosProtocol.encodeEnvelope(envelope))
    }

    fun broadcastAttestation(attestation: AdminAttestation) {
        val envelope = SosEnvelope(
            messageId = UUID.randomUUID().toString(),
            type = MessageType.ADMIN_ATTESTATION,
            senderPeerId = attestation.adminPeerId,
            timestampEpochMs = System.currentTimeMillis(),
            payload = SosProtocol.json.encodeToString(attestation),
        )
        sendBytes(SosProtocol.encodeEnvelope(envelope))
    }

    fun onPayloadReceived(bytes: ByteArray) {
        scope.launch { handlePayload(bytes) }
    }

    private suspend fun announceIdentity() {
        val identity = identityRepository.getIdentity() ?: return
        val payload = identityRepository.toAnnouncePayload(identity)
        val envelope = SosEnvelope(
            messageId = UUID.randomUUID().toString(),
            type = MessageType.IDENTITY_ANNOUNCE,
            senderPeerId = identity.peerId,
            timestampEpochMs = System.currentTimeMillis(),
            payload = SosProtocol.json.encodeToString(payload),
        )
        sendBytes(SosProtocol.encodeEnvelope(envelope))
    }

    private fun sendBytes(bytes: ByteArray) {
        val service = meshService
        if (service != null) {
            service.broadcast(bytes)
        } else {
            pendingOutbound.add(bytes)
            SosMeshService.pendingBroadcast = bytes
            startMesh()
        }
    }

    private fun flushPendingOutbound() {
        while (true) {
            val next = pendingOutbound.poll() ?: break
            meshService?.broadcast(next)
        }
    }

    private suspend fun handlePayload(bytes: ByteArray) {
        val envelope = try {
            SosProtocol.decodeEnvelope(bytes)
        } catch (_: Exception) {
            return
        }

        if (alertRepository.hasSeenMessage(envelope.messageId)) {
            return
        }
        alertRepository.markSeen(envelope.messageId)

        val selfPeerId = identityRepository.getIdentity()?.peerId
        val isOwnSos =
            envelope.type == MessageType.SOS_ALERT && envelope.senderPeerId == selfPeerId

        when (envelope.type) {
            MessageType.SOS_ALERT -> {
                if (!isOwnSos) handleSosAlert(envelope)
            }
            MessageType.SOS_ACK -> handleSosAck(envelope)
            MessageType.VERIFICATION_REQUEST -> handleVerificationRequest(envelope)
            MessageType.ADMIN_ATTESTATION -> handleAdminAttestation(envelope)
            MessageType.IDENTITY_ANNOUNCE -> Unit
            else -> Unit
        }

        if (!isOwnSos) relayIfNeeded(envelope)
    }

    private suspend fun handleSosAlert(envelope: SosEnvelope) {
        val payload = try {
            SosProtocol.json.decodeFromString<SosAlertPayload>(envelope.payload)
        } catch (_: Exception) {
            return
        }

        val voicePath = payload.voiceNoteBase64?.let { b64 ->
            VoiceNoteStorage.save(context, payload.alertId, Base64.getDecoder().decode(b64))
        }

        alertRepository.saveIncomingAlert(payload, envelope.senderPeerId, voicePath)
        _incomingAlerts.emit(payload)
        SosAlertNotifier.show(context, payload, voicePath)
    }

    private suspend fun handleSosAck(envelope: SosEnvelope) {
        if (envelope.senderPeerId == identityRepository.getIdentity()?.peerId) return
        val payload = try {
            SosProtocol.json.decodeFromString<SosAckPayload>(envelope.payload)
        } catch (_: Exception) {
            return
        }
        val name = payload.responderDisplayName ?: payload.responderPeerId.take(8)
        val text = payload.message ?: context.getString(org.sosnetwork.app.R.string.responder_on_way)
        SosAlertNotifier.showAck(context, payload.alertId, name, text)
    }

    private suspend fun handleAdminAttestation(envelope: SosEnvelope) {
        val attestation = try {
            SosProtocol.json.decodeFromString<AdminAttestation>(envelope.payload)
        } catch (_: Exception) {
            return
        }
        val trusted = identityRepository.trustedAdminKeys()
        if (!AttestationValidator.verifyAttestation(attestation, trusted)) return

        val self = identityRepository.getIdentity() ?: return
        if (attestation.subjectPeerId != self.peerId) return

        val existing = identityRepository.parseAttestations(self)
        val merged = existing.filter { it.attestationType != attestation.attestationType } + attestation
        val updatedLevel = when (attestation.attestationType) {
            AttestationType.PHYSICAL_PRESENCE_VERIFIED -> VerificationLevel.PHYSICALLY_VERIFIED
            AttestationType.GOVERNMENT_ID_VERIFIED -> VerificationLevel.ID_VERIFIED
        }
        identityRepository.updateVerificationLevel(self.peerId, updatedLevel, merged)

        verificationRepository.syncVerificationFromAttestation(attestation)
    }

    private suspend fun handleVerificationRequest(envelope: SosEnvelope) {
        val payload = try {
            SosProtocol.json.decodeFromString<VerificationRequestPayload>(envelope.payload)
        } catch (_: Exception) {
            return
        }
        verificationRepository.upsertFromMesh(
            VerificationRequestEntity(
                peerId = payload.peerId,
                displayName = payload.displayName,
                governmentIdHash = payload.governmentIdHash,
                idDocumentType = payload.idDocumentType.name,
                encryptedDocumentPath = "",
                submittedAtEpochMs = payload.submittedAtEpochMs,
                idVerified = false,
                physicallyVerified = false,
                adminNotes = null,
            ),
        )
    }

    private fun relayIfNeeded(envelope: SosEnvelope) {
        if (envelope.hopCount >= SosProtocol.MAX_HOP_COUNT) return
        val relayed = envelope.copy(hopCount = (envelope.hopCount + 1).toByte())
        val relayBytes = SosProtocol.encodeEnvelope(relayed)
        meshService?.relay(envelope.messageId, relayBytes)
    }

    private fun capVoicePayload(bytes: ByteArray): ByteArray {
        val max = 256 * 1024
        return if (bytes.size <= max) bytes else bytes.copyOf(max)
    }
}
