package org.sosnetwork.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SOS Decentralized Protocol (SOS-DP) v1.0
 *
 * Peer-to-peer mesh for emergency alerts. No central server required for
 * alert propagation; identity attestations are signed by verified admins.
 */
object SosProtocol {
    const val VERSION: Byte = 1
    const val MAGIC = "SOS1"
    const val MAX_HOP_COUNT: Byte = 8
    const val MAX_PAYLOAD_BYTES = 64 * 1024

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeEnvelope(envelope: SosEnvelope): ByteArray {
        val body = json.encodeToString(envelope).toByteArray(Charsets.UTF_8)
        require(body.size <= MAX_PAYLOAD_BYTES) { "Payload exceeds ${MAX_PAYLOAD_BYTES} bytes" }
        return MAGIC.toByteArray(Charsets.UTF_8) + byteArrayOf(VERSION) + body
    }

    fun decodeEnvelope(bytes: ByteArray): SosEnvelope {
        val magic = bytes.copyOfRange(0, 4).toString(Charsets.UTF_8)
        require(magic == MAGIC) { "Invalid SOS-DP magic" }
        val version = bytes[4]
        require(version == VERSION) { "Unsupported protocol version: $version" }
        return json.decodeFromString(bytes.copyOfRange(5, bytes.size).toString(Charsets.UTF_8))
    }
}

@Serializable
data class SosEnvelope(
    @SerialName("id") val messageId: String,
    @SerialName("type") val type: MessageType,
    @SerialName("sender") val senderPeerId: String,
    @SerialName("ts") val timestampEpochMs: Long,
    @SerialName("hop") val hopCount: Byte = 0,
    @SerialName("ttl") val ttlSeconds: Int = 300,
    @SerialName("sig") val signatureBase64: String? = null,
    @SerialName("payload") val payload: String,
)

@Serializable
enum class MessageType {
    @SerialName("HELLO") HELLO,
    @SerialName("SOS_ALERT") SOS_ALERT,
    @SerialName("SOS_ACK") SOS_ACK,
    @SerialName("RELAY") RELAY,
    @SerialName("IDENTITY_ANNOUNCE") IDENTITY_ANNOUNCE,
    @SerialName("ADMIN_ATTESTATION") ADMIN_ATTESTATION,
    @SerialName("VERIFICATION_REQUEST") VERIFICATION_REQUEST,
}

@Serializable
data class VerificationRequestPayload(
    val peerId: String,
    val displayName: String,
    val governmentIdHash: String,
    val idDocumentType: GovernmentIdType,
    val submittedAtEpochMs: Long,
)

@Serializable
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val altitudeMeters: Double? = null,
)

@Serializable
data class SosAlertPayload(
    val alertId: String,
    val location: GeoLocation,
    val messageText: String? = null,
    val voiceNoteMime: String? = null,
    val voiceNoteBase64: String? = null,
    val severity: AlertSeverity = AlertSeverity.CRITICAL,
    val senderDisplayName: String? = null,
    val senderVerificationLevel: VerificationLevel = VerificationLevel.UNVERIFIED,
)

@Serializable
enum class AlertSeverity {
    @SerialName("LOW") LOW,
    @SerialName("MEDIUM") MEDIUM,
    @SerialName("HIGH") HIGH,
    @SerialName("CRITICAL") CRITICAL,
}

@Serializable
enum class VerificationLevel {
    @SerialName("UNVERIFIED") UNVERIFIED,
    @SerialName("ID_SUBMITTED") ID_SUBMITTED,
    @SerialName("ID_VERIFIED") ID_VERIFIED,
    @SerialName("PHYSICALLY_VERIFIED") PHYSICALLY_VERIFIED,
}

@Serializable
data class IdentityAnnouncePayload(
    val peerId: String,
    val publicKeyBase64: String,
    val displayName: String,
    val governmentIdHash: String,
    val idDocumentType: GovernmentIdType,
    val verificationLevel: VerificationLevel,
    val adminAttestations: List<AdminAttestation> = emptyList(),
)

@Serializable
enum class GovernmentIdType {
    @SerialName("PASSPORT") PASSPORT,
    @SerialName("NATIONAL_ID") NATIONAL_ID,
    @SerialName("DRIVERS_LICENSE") DRIVERS_LICENSE,
    @SerialName("RESIDENCE_PERMIT") RESIDENCE_PERMIT,
    @SerialName("OTHER") OTHER,
}

@Serializable
data class AdminAttestation(
    val adminPeerId: String,
    val adminPublicKeyBase64: String,
    val subjectPeerId: String,
    val attestationType: AttestationType,
    val issuedAtEpochMs: Long,
    val expiresAtEpochMs: Long? = null,
    val signatureBase64: String,
    val notes: String? = null,
)

@Serializable
enum class AttestationType {
    @SerialName("GOVERNMENT_ID_VERIFIED") GOVERNMENT_ID_VERIFIED,
    @SerialName("PHYSICAL_PRESENCE_VERIFIED") PHYSICAL_PRESENCE_VERIFIED,
}

@Serializable
data class HelloPayload(
    val peerId: String,
    val publicKeyBase64: String,
    val protocolVersion: Byte = SosProtocol.VERSION,
    val capabilities: List<String> = listOf("NEARBY", "RELAY"),
)

@Serializable
data class SosAckPayload(
    val alertId: String,
    val responderPeerId: String,
    val responderDisplayName: String? = null,
    val message: String? = null,
    val etaMinutes: Int? = null,
)
