package com.epalma.tvespanolplus

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small AES/GCM vault backed by AndroidKeyStore.
 * Sensitive playlist JSON and cached M3U/XMLTV bytes are encrypted at rest.
 */
class SecureStore(context: Context) {
    private val prefs = context.getSharedPreferences("tv_espanol_plus_secure", Context.MODE_PRIVATE)
    private val alias = "tv_espanol_plus_aes_v1"

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, encode(encrypt(value.toByteArray(Charsets.UTF_8)))).apply()
    }

    fun getString(key: String): String? = prefs.getString(key, null)?.let {
        runCatching { decrypt(decode(it)).toString(Charsets.UTF_8) }.getOrNull()
    }

    fun remove(key: String) { prefs.edit().remove(key).apply() }

    fun encryptBytes(bytes: ByteArray): ByteArray = encrypt(bytes)
    fun decryptBytes(bytes: ByteArray): ByteArray = decrypt(bytes)

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain)
        return byteArrayOf(iv.size.toByte()) + iv + encrypted
    }

    private fun decrypt(blob: ByteArray): ByteArray {
        require(blob.isNotEmpty()) { "Datos cifrados vacíos" }
        val ivLength = blob[0].toInt() and 0xFF
        require(ivLength in 12..16 && blob.size > 1 + ivLength) { "Datos cifrados inválidos" }
        val iv = blob.copyOfRange(1, 1 + ivLength)
        val encrypted = blob.copyOfRange(1 + ivLength, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)
}
