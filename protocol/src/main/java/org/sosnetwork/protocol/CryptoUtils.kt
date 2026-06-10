package org.sosnetwork.protocol

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.security.Signature
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val KEY_ALGO = "EC"
    private const val SIGN_ALGO = "SHA256withECDSA"
    private const val AES_GCM = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_BYTES = 12

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    fun generateKeyPair(): KeyPair {
        val gen = KeyPairGenerator.getInstance(KEY_ALGO)
        gen.initialize(256)
        return gen.generateKeyPair()
    }

    fun publicKeyToBase64(publicKey: PublicKey): String =
        Base64.getEncoder().encodeToString(publicKey.encoded)

    fun privateKeyToBase64(privateKey: PrivateKey): String =
        Base64.getEncoder().encodeToString(privateKey.encoded)

    fun publicKeyFromBase64(encoded: String): PublicKey {
        val bytes = Base64.getDecoder().decode(encoded)
        return java.security.KeyFactory.getInstance(KEY_ALGO)
            .generatePublic(java.security.spec.X509EncodedKeySpec(bytes))
    }

    fun privateKeyFromBase64(encoded: String): PrivateKey {
        val bytes = Base64.getDecoder().decode(encoded)
        return java.security.KeyFactory.getInstance(KEY_ALGO)
            .generatePrivate(java.security.spec.PKCS8EncodedKeySpec(bytes))
    }

    fun sign(data: ByteArray, privateKey: PrivateKey): String {
        val sig = Signature.getInstance(SIGN_ALGO)
        sig.initSign(privateKey)
        sig.update(data)
        return Base64.getEncoder().encodeToString(sig.sign())
    }

    fun verify(data: ByteArray, signatureBase64: String, publicKey: PublicKey): Boolean {
        return try {
            val sig = Signature.getInstance(SIGN_ALGO)
            sig.initVerify(publicKey)
            sig.update(data)
            sig.verify(Base64.getDecoder().decode(signatureBase64))
        } catch (_: Exception) {
            false
        }
    }

    /** SHA-256 hash of government ID document bytes — raw ID never leaves device in alerts. */
    fun hashGovernmentId(documentBytes: ByteArray, peerId: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        digest.update(peerId.toByteArray(Charsets.UTF_8))
        digest.update(documentBytes)
        return Base64.getEncoder().encodeToString(digest.digest())
    }

    fun generateAesKey(): SecretKey {
        val gen = KeyGenerator.getInstance("AES")
        gen.init(256)
        return gen.generateKey()
    }

    fun encryptAesGcm(plain: ByteArray, key: SecretKey): ByteArray {
        val iv = ByteArray(GCM_IV_BYTES).also { java.security.SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherText = cipher.doFinal(plain)
        return iv + cipherText
    }

    fun decryptAesGcm(encrypted: ByteArray, key: SecretKey): ByteArray {
        val iv = encrypted.copyOfRange(0, GCM_IV_BYTES)
        val cipherText = encrypted.copyOfRange(GCM_IV_BYTES, encrypted.size)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(cipherText)
    }

    fun secretKeyFromBytes(bytes: ByteArray): SecretKey = SecretKeySpec(bytes, "AES")
}
