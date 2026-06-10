package org.sosnetwork.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "identity")
data class IdentityEntity(
    @PrimaryKey val peerId: String,
    val displayName: String,
    val publicKeyBase64: String,
    val privateKeyBase64: String,
    val governmentIdHash: String?,
    val idDocumentType: String?,
    val verificationLevel: String,
    val isAdmin: Boolean,
    val adminAttestationsJson: String,
    val createdAtEpochMs: Long,
)

@Entity(tableName = "verification_requests")
data class VerificationRequestEntity(
    @PrimaryKey val peerId: String,
    val displayName: String,
    val governmentIdHash: String,
    val idDocumentType: String,
    val encryptedDocumentPath: String,
    val submittedAtEpochMs: Long,
    val idVerified: Boolean,
    val physicallyVerified: Boolean,
    val adminNotes: String?,
)

@Entity(tableName = "received_alerts")
data class ReceivedAlertEntity(
    @PrimaryKey val alertId: String,
    val senderPeerId: String,
    val senderDisplayName: String?,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val messageText: String?,
    val voiceNotePath: String?,
    val severity: String,
    val verificationLevel: String,
    val receivedAtEpochMs: Long,
    val acknowledged: Boolean,
)

@Entity(tableName = "seen_messages")
data class SeenMessageEntity(
    @PrimaryKey val messageId: String,
    val seenAtEpochMs: Long,
)
