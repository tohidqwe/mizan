package com.mizan.civilleitner.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecureDatabaseKeyStore {
    private const val STORE = "AndroidKeyStore"
    private const val ALIAS = "dr_tohid_client_db_wrap_v1"
    private const val PREFS = "secure_client_database"
    private const val PREF_IV = "wrapped_db_key_iv"
    private const val PREF_CIPHERTEXT = "wrapped_db_key_ciphertext"

    fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val storedIv = prefs.getString(PREF_IV, null)
        val storedCiphertext = prefs.getString(PREF_CIPHERTEXT, null)

        if (storedIv != null && storedCiphertext != null) {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateWrappingKey(),
                GCMParameterSpec(128, Base64.getDecoder().decode(storedIv)),
            )
            return cipher.doFinal(Base64.getDecoder().decode(storedCiphertext))
        }

        val passphrase = ByteArray(32).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateWrappingKey())
        val encrypted = cipher.doFinal(passphrase)

        prefs.edit()
            .putString(PREF_IV, Base64.getEncoder().encodeToString(cipher.iv))
            .putString(PREF_CIPHERTEXT, Base64.getEncoder().encodeToString(encrypted))
            .commit()

        return passphrase
    }

    private fun getOrCreateWrappingKey(): SecretKey {
        val store = KeyStore.getInstance(STORE).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
