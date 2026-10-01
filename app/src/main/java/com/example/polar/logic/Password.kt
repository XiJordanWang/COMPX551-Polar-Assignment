package com.example.polar.logic

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// We never store the real password. We store "salt:hash".
// - salt: 16 random bytes, different for every user, so two people with the
//   same password still get different hashes.
// - hash: PBKDF2 runs the password + salt through HMAC-SHA1 10,000 times,
//   which makes guessing passwords very slow for an attacker.
// (PBKDF2WithHmacSHA1 works on every Android version we support, API 24+.)

private const val ITERATIONS = 10000
private const val KEY_LENGTH_BITS = 256

// "123456" -> "9f2c...:a81b..."
fun hashPassword(password: String): String {
    val salt = ByteArray(16)
    SecureRandom().nextBytes(salt)
    val hash = pbkdf2(password, salt)
    return toHex(salt) + ":" + toHex(hash)
}

// true if this password matches the saved "salt:hash"
fun checkPassword(password: String, saved: String): Boolean {
    val parts = saved.split(":")
    if (parts.size != 2) return false
    return try {
        val salt = fromHex(parts[0])
        val hash = pbkdf2(password, salt)
        toHex(hash) == parts[1]
    } catch (e: NumberFormatException) {
        // The saved value is not valid hex, so it can't match
        false
    }
}

private fun pbkdf2(password: String, salt: ByteArray): ByteArray {
    val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    return factory.generateSecret(spec).encoded
}

// Bytes -> text like "0fa3", so we can save it as a string
private fun toHex(bytes: ByteArray): String {
    return bytes.joinToString("") { "%02x".format(it) }
}

private fun fromHex(text: String): ByteArray {
    return ByteArray(text.length / 2) { i -> text.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
}
