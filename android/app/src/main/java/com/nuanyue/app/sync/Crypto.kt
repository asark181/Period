package com.nuanyue.app.sync

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-end encryption for cloud backup. The server only ever sees:
 *  - an opaque space id  (sha256 of the account name)
 *  - an auth token       (half of the PBKDF2 output; server stores only its hash)
 *  - an AES-GCM ciphertext
 * The encryption key (other half) never leaves the phone.
 */
object Crypto {
    private const val ITERATIONS = 120_000

    data class Keys(val encKey: ByteArray, val authToken: ByteArray)

    fun normalizeAccount(account: String) = account.trim().lowercase()

    fun spaceId(account: String): String = sha256Hex("nuanyue:space:" + normalizeAccount(account))

    fun deriveKeyMaterial(account: String, password: String): ByteArray {
        val salt = ("nuanyue:salt:" + normalizeAccount(account)).toByteArray()
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 512)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    fun split(material: ByteArray) = Keys(material.copyOfRange(0, 32), material.copyOfRange(32, 64))

    fun encrypt(key: ByteArray, plain: ByteArray): ByteArray {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return iv + cipher.doFinal(plain)
    }

    fun decrypt(key: ByteArray, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, data, 0, 12))
        return cipher.doFinal(data, 12, data.size - 12)
    }

    fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

    private fun sha256Hex(s: String) = hex(MessageDigest.getInstance("SHA-256").digest(s.toByteArray()))
}
