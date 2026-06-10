package org.sosnetwork.app.security

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import org.sosnetwork.protocol.CryptoUtils
import java.io.File

/**
 * Stores government ID images encrypted at rest. Documents are only decrypted
 * on-device for admin review; they are never included in SOS mesh broadcasts.
 */
class SecureDocumentStore(private val context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val dir = File(context.filesDir, "secure_ids").apply { mkdirs() }

    fun encryptAndStore(peerId: String, documentBytes: ByteArray): String {
        val file = File(dir, "$peerId.id.enc")
        val aesKey = CryptoUtils.generateAesKey()
        val encrypted = CryptoUtils.encryptAesGcm(documentBytes, aesKey)
        val keyFile = File(dir, "$peerId.key.enc")

        writeEncrypted(keyFile, aesKey.encoded)
        writeEncrypted(file, encrypted)
        return file.absolutePath
    }

    fun decrypt(documentFile: File): ByteArray? {
        val peerId = documentFile.name.removeSuffix(".id.enc")
        val keyFile = File(dir, "$peerId.key.enc")
        if (!keyFile.exists()) return null
        val keyBytes = readEncrypted(keyFile)
        val docBytes = readEncrypted(documentFile)
        val key = CryptoUtils.secretKeyFromBytes(keyBytes)
        return CryptoUtils.decryptAesGcm(docBytes, key)
    }

    private fun writeEncrypted(file: File, bytes: ByteArray) {
        EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build().openFileOutput().use { it.write(bytes) }
    }

    private fun readEncrypted(file: File): ByteArray =
        EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build().openFileInput().use { it.readBytes() }
}
