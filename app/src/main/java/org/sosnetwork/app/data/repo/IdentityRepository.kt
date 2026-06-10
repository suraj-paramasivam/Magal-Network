package org.sosnetwork.app.data.repo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.sosnetwork.app.data.local.IdentityDao
import org.sosnetwork.app.data.local.IdentityEntity
import org.sosnetwork.protocol.AdminAttestation
import org.sosnetwork.protocol.CryptoUtils
import org.sosnetwork.protocol.GovernmentIdType
import org.sosnetwork.protocol.IdentityAnnouncePayload
import org.sosnetwork.protocol.SosProtocol
import org.sosnetwork.protocol.VerificationLevel
import java.util.UUID

private val Context.trustedAdminsStore by preferencesDataStore("trusted_admins")

class IdentityRepository(
    private val context: Context,
    private val identityDao: IdentityDao,
) {
    val identityFlow = identityDao.observeIdentity()

    suspend fun getIdentity(): IdentityEntity? = identityDao.getIdentity()

    suspend fun isRegistered(): Boolean = getIdentity() != null

    suspend fun register(displayName: String): IdentityEntity {
        val keyPair = CryptoUtils.generateKeyPair()
        val peerId = UUID.randomUUID().toString()
        val entity = IdentityEntity(
            peerId = peerId,
            displayName = displayName.trim(),
            publicKeyBase64 = CryptoUtils.publicKeyToBase64(keyPair.public),
            privateKeyBase64 = CryptoUtils.privateKeyToBase64(keyPair.private),
            governmentIdHash = null,
            idDocumentType = null,
            verificationLevel = VerificationLevel.UNVERIFIED.name,
            isAdmin = false,
            adminAttestationsJson = "[]",
            createdAtEpochMs = System.currentTimeMillis(),
        )
        identityDao.upsert(entity)
        return entity
    }

    suspend fun updateVerificationLevel(peerId: String, level: VerificationLevel, attestations: List<AdminAttestation>) {
        val current = identityDao.getIdentity() ?: return
        if (current.peerId != peerId) return
        identityDao.upsert(
            current.copy(
                verificationLevel = level.name,
                adminAttestationsJson = SosProtocol.json.encodeToString(
                    ListSerializer(AdminAttestation.serializer()),
                    attestations,
                ),
            )
        )
    }

    suspend fun setGovernmentId(peerId: String, hash: String, type: GovernmentIdType) {
        val current = identityDao.getIdentity() ?: return
        identityDao.upsert(
            current.copy(
                governmentIdHash = hash,
                idDocumentType = type.name,
                verificationLevel = VerificationLevel.ID_SUBMITTED.name,
            )
        )
    }

    suspend fun promoteToAdmin(peerId: String) {
        val current = identityDao.getIdentity() ?: return
        identityDao.upsert(current.copy(isAdmin = true))
    }

    fun parseAttestations(entity: IdentityEntity): List<AdminAttestation> =
        try {
            SosProtocol.json.decodeFromString(
                ListSerializer(AdminAttestation.serializer()),
                entity.adminAttestationsJson,
            )
        } catch (_: Exception) {
            emptyList()
        }

    fun toAnnouncePayload(entity: IdentityEntity): IdentityAnnouncePayload =
        IdentityAnnouncePayload(
            peerId = entity.peerId,
            publicKeyBase64 = entity.publicKeyBase64,
            displayName = entity.displayName,
            governmentIdHash = entity.governmentIdHash ?: "",
            idDocumentType = entity.idDocumentType?.let {
                runCatching { GovernmentIdType.valueOf(it) }.getOrDefault(GovernmentIdType.OTHER)
            } ?: GovernmentIdType.OTHER,
            verificationLevel = runCatching {
                VerificationLevel.valueOf(entity.verificationLevel)
            }.getOrDefault(VerificationLevel.UNVERIFIED),
            adminAttestations = parseAttestations(entity),
        )

    suspend fun trustedAdminKeys(): Set<String> =
        context.trustedAdminsStore.data.map { prefs ->
            prefs[TRUSTED_ADMIN_KEYS] ?: emptySet()
        }.first()

    suspend fun addTrustedAdmin(publicKeyBase64: String) {
        context.trustedAdminsStore.edit { prefs ->
            val current = prefs[TRUSTED_ADMIN_KEYS]?.toMutableSet() ?: mutableSetOf()
            current.add(publicKeyBase64)
            prefs[TRUSTED_ADMIN_KEYS] = current
        }
    }

    companion object {
        private val TRUSTED_ADMIN_KEYS = stringSetPreferencesKey("trusted_admin_public_keys")
    }
}
