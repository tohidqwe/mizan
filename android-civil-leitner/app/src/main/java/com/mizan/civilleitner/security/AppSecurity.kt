package com.mizan.civilleitner.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AppSecurityStore(context: Context) {
    private val prefs = context.getSharedPreferences("app_security", Context.MODE_PRIVATE)

    var userLockEnabled: Boolean
        get() = prefs.getBoolean("user_lock_enabled", false)
        set(value) = prefs.edit().putBoolean("user_lock_enabled", value).apply()

    fun hasPin(): Boolean = prefs.contains("pin_hash") && prefs.contains("pin_salt")

    fun setPin(pin: CharArray) {
        require(pin.size >= 4) { "PIN must contain at least four characters" }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val hash = derivePin(pin, salt)
        prefs.edit()
            .putString("pin_salt", Base64.getEncoder().encodeToString(salt))
            .putString("pin_hash", Base64.getEncoder().encodeToString(hash))
            .apply()
        pin.fill('\u0000')
    }

    fun verifyPin(pin: CharArray): Boolean {
        val salt = prefs.getString("pin_salt", null)?.let(Base64.getDecoder()::decode) ?: return false
        val expected = prefs.getString("pin_hash", null)?.let(Base64.getDecoder()::decode) ?: return false
        val actual = derivePin(pin, salt)
        pin.fill('\u0000')
        return MessageDigest.isEqual(expected, actual)
    }

    fun clearPin() {
        prefs.edit().remove("pin_salt").remove("pin_hash").apply()
    }

    var adminDeviceId: String?
        get() = prefs.getString("admin_device_id", null)
        set(value) {
            if (value == null) prefs.edit().remove("admin_device_id").apply()
            else prefs.edit().putString("admin_device_id", value).apply()
        }

    var adminApiBaseUrl: String?
        get() = prefs.getString("admin_api_base_url", null)
        set(value) {
            if (value.isNullOrBlank()) prefs.edit().remove("admin_api_base_url").apply()
            else prefs.edit().putString("admin_api_base_url", value.trimEnd('/')).apply()
        }

    fun isAdminBound(): Boolean = !adminDeviceId.isNullOrBlank() && AdminDeviceKeyStore.hasKey()

    private fun derivePin(pin: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin, salt, 150_000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}

object AdminDeviceKeyStore {
    private const val STORE = "AndroidKeyStore"
    private const val ALIAS = "dr_tohid_admin_signing_v1"

    fun hasKey(): Boolean = runCatching {
        KeyStore.getInstance(STORE).apply { load(null) }.containsAlias(ALIAS)
    }.getOrDefault(false)

    fun ensureKey() {
        if (hasKey()) return
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, STORE)
        val builder = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG,
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }
        generator.initialize(builder.build())
        generator.generateKeyPair()
    }

    fun deleteKey() {
        runCatching {
            KeyStore.getInstance(STORE).apply { load(null) }.deleteEntry(ALIAS)
        }
    }

    fun publicKeyPem(): String {
        ensureKey()
        val store = KeyStore.getInstance(STORE).apply { load(null) }
        val encoded = store.getCertificate(ALIAS).publicKey.encoded
        val base64 = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(encoded)
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----"
    }

    fun prepareSignature(): Signature {
        ensureKey()
        val store = KeyStore.getInstance(STORE).apply { load(null) }
        val privateKey = store.getKey(ALIAS, null) as java.security.PrivateKey
        return Signature.getInstance("SHA256withECDSA").apply {
            initSign(privateKey)
        }
    }
}
