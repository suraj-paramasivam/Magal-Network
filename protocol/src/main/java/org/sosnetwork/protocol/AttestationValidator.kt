package org.sosnetwork.protocol

import kotlinx.serialization.encodeToString

/**
 * Validates admin attestations on the device without a central server.
 * Admins are trusted by their public key being pinned at install or via QR bootstrap.
 */
object AttestationValidator {

    fun isAdminTrusted(adminPublicKeyBase64: String, trustedAdminKeys: Set<String>): Boolean =
        adminPublicKeyBase64 in trustedAdminKeys

    fun verifyAttestation(
        attestation: AdminAttestation,
        trustedAdminKeys: Set<String>,
    ): Boolean {
        if (!isAdminTrusted(attestation.adminPublicKeyBase64, trustedAdminKeys)) return false
        attestation.expiresAtEpochMs?.let { if (System.currentTimeMillis() > it) return false }

        val payloadBytes = buildAttestationSignPayload(attestation)
        val adminKey = try {
            CryptoUtils.publicKeyFromBase64(attestation.adminPublicKeyBase64)
        } catch (_: Exception) {
            return false
        }
        return CryptoUtils.verify(payloadBytes, attestation.signatureBase64, adminKey)
    }

    fun buildAttestationSignPayload(attestation: AdminAttestation): ByteArray {
        val canonical = SosProtocol.json.encodeToString(
            attestation.copy(signatureBase64 = "")
        )
        return canonical.toByteArray(Charsets.UTF_8)
    }

    fun effectiveVerificationLevel(
        identity: IdentityAnnouncePayload,
        trustedAdminKeys: Set<String>,
    ): VerificationLevel {
        var level = identity.verificationLevel
        for (att in identity.adminAttestations) {
            if (!verifyAttestation(att, trustedAdminKeys)) continue
            level = when (att.attestationType) {
                AttestationType.GOVERNMENT_ID_VERIFIED ->
                    maxOf(level, VerificationLevel.ID_VERIFIED, VerificationLevel.ID_SUBMITTED)
                AttestationType.PHYSICAL_PRESENCE_VERIFIED ->
                    VerificationLevel.PHYSICALLY_VERIFIED
            }
        }
        return level
    }
}

private fun maxOf(a: VerificationLevel, vararg others: VerificationLevel): VerificationLevel {
    val order = VerificationLevel.entries
    return (listOf(a) + others).maxBy { order.indexOf(it) }
}
