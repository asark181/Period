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

    // ---- Invite codes: one code = one space. Account and password are both derived from it. ----

    private const val ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"

    /** "ny-7k3p q9xm 4t2r" -> "NY7K3PQ9XM4T2R", or null if it is not a valid code. */
    fun normalizeInvite(code: String): String? {
        val c = code.uppercase().filter { it.isLetterOrDigit() }
        return c.takeIf { it.length == 14 && it.startsWith("NY") && it.drop(2).all { ch -> ch in ALPHABET } }
    }

    fun inviteAccount(normalized: String) = "inv-" + normalized.lowercase()

    fun isInviteAccount(account: String) = account.startsWith("inv-")

    /** "inv-ny7k3pq9xm4t2r" -> "NY-7K3P-Q9XM-4T2R" */
    fun displayInvite(account: String): String {
        val c = account.removePrefix("inv-").uppercase()
        return listOf(c.take(2), c.substring(2, 6), c.substring(6, 10), c.substring(10)).joinToString("-")
    }

    fun newInvite(): String {
        val rnd = SecureRandom()
        return "NY" + (1..12).map { ALPHABET[rnd.nextInt(ALPHABET.length)] }.joinToString("")
    }

    fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

    private fun sha256Hex(s: String) = hex(MessageDigest.getInstance("SHA-256").digest(s.toByteArray()))
}
