package org.sosnetwork.app.data.repo

import android.content.Context
import kotlinx.coroutines.flow.Flow
import org.sosnetwork.app.data.local.VerificationDao
import org.sosnetwork.app.data.local.VerificationRequestEntity
import org.sosnetwork.app.security.SecureDocumentStore
import org.sosnetwork.protocol.AdminAttestation
import org.sosnetwork.protocol.AttestationType
import org.sosnetwork.protocol.CryptoUtils
import org.sosnetwork.protocol.GovernmentIdType
import org.sosnetwork.protocol.VerificationLevel
import java.io.File
import java.security.PrivateKey

class VerificationRepository(
    private val context: Context,
    private val verificationDao: VerificationDao,
) {
    private val documentStore = SecureDocumentStore(context)

    fun observePending(): Flow<List<VerificationRequestEntity>> = verificationDao.observePending()

    suspend fun upsertFromMesh(entity: VerificationRequestEntity) {
        val existing = verificationDao.get(entity.peerId)
        if (existing != null && existing.encryptedDocumentPath.isNotEmpty()) return
        verificationDao.upsert(entity.copy(encryptedDocumentPath = existing?.encryptedDocumentPath ?: ""))
    }

    suspend fun submitGovernmentId(
        peerId: String,
        displayName: String,
        documentBytes: ByteArray,
        idType: GovernmentIdType,
    ): String {
        val hash = CryptoUtils.hashGovernmentId(documentBytes, peerId)
        val path = documentStore.encryptAndStore(peerId, documentBytes)
        verificationDao.upsert(
            VerificationRequestEntity(
                peerId = peerId,
                displayName = displayName,
                governmentIdHash = hash,
                idDocumentType = idType.name,
                encryptedDocumentPath = path,
                submittedAtEpochMs = System.currentTimeMillis(),
                idVerified = false,
                physicallyVerified = false,
                adminNotes = null,
            )
        )
        return hash
    }

    suspend fun getRequest(peerId: String): VerificationRequestEntity? =
        verificationDao.get(peerId)

    suspend fun adminVerifyGovernmentId(peerId: String, notes: String?) {
        verificationDao.updateVerification(peerId, idVerified = true, physicallyVerified = false, notes = notes)
    }

    suspend fun adminVerifyPhysicalPresence(peerId: String, notes: String?) {
        verificationDao.updateVerification(peerId, idVerified = true, physicallyVerified = true, notes = notes)
    }

    fun loadEncryptedDocument(path: String): ByteArray? {
        val file = File(path)
        if (!file.exists()) return null
        return documentStore.decrypt(file)
    }

    fun createAdminAttestation(
        adminPeerId: String,
        adminPublicKeyBase64: String,
        adminPrivateKey: PrivateKey,
        subjectPeerId: String,
        type: AttestationType,
        notes: String?,
    ): org.sosnetwork.protocol.AdminAttestation {
        val draft = org.sosnetwork.protocol.AdminAttestation(
            adminPeerId = adminPeerId,
            adminPublicKeyBase64 = adminPublicKeyBase64,
            subjectPeerId = subjectPeerId,
            attestationType = type,
            issuedAtEpochMs = System.currentTimeMillis(),
            expiresAtEpochMs = System.currentTimeMillis() + ATTESTATION_VALIDITY_MS,
            signatureBase64 = "",
            notes = notes,
        )
        val sig = CryptoUtils.sign(
            org.sosnetwork.protocol.AttestationValidator.buildAttestationSignPayload(draft),
            adminPrivateKey,
        )
        return draft.copy(signatureBase64 = sig)
    }

    suspend fun syncVerificationFromAttestation(attestation: AdminAttestation) {
        val existing = verificationDao.get(attestation.subjectPeerId) ?: return
        when (attestation.attestationType) {
            AttestationType.GOVERNMENT_ID_VERIFIED ->
                verificationDao.updateVerification(
                    attestation.subjectPeerId,
                    idVerified = true,
                    physicallyVerified = existing.physicallyVerified,
                    notes = attestation.notes,
                )
            AttestationType.PHYSICAL_PRESENCE_VERIFIED ->
                verificationDao.updateVerification(
                    attestation.subjectPeerId,
                    idVerified = true,
                    physicallyVerified = true,
                    notes = attestation.notes,
                )
        }
    }

    fun verificationLevelFromRequest(req: VerificationRequestEntity): VerificationLevel =
        when {
            req.physicallyVerified -> VerificationLevel.PHYSICALLY_VERIFIED
            req.idVerified -> VerificationLevel.ID_VERIFIED
            else -> VerificationLevel.ID_SUBMITTED
        }

    companion object {
        private const val ATTESTATION_VALIDITY_MS = 365L * 24 * 60 * 60 * 1000
    }
}
