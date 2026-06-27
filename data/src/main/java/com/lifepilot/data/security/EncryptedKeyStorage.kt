package com.lifepilot.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedKeyStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val keystore = KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }
    private val keyAlias = "lifepilot_secrets"
    private val sharedPrefs = context.getSharedPreferences("lifepilot_encrypted", Context.MODE_PRIVATE)

    private fun getOrCreateKey(): SecretKey {
        return if (keystore.containsAlias(keyAlias)) {
            (keystore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
        } else {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            keyGenerator.generateKey()
        }
    }

    fun store(key: String, value: String) {
        try {
            val secretKey = getOrCreateKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            val combined = iv + encrypted
            sharedPrefs.edit()
                .putString(key, Base64.encodeToString(combined, Base64.NO_WRAP))
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to encrypt value for key: $key")
        }
    }

    fun retrieve(key: String): String? {
        return try {
            val stored = sharedPrefs.getString(key, null) ?: return null
            val combined = Base64.decode(stored, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, 12)
            val encrypted = combined.copyOfRange(12, combined.size)
            val secretKey = getOrCreateKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            Timber.e(e, "Failed to decrypt value for key: $key")
            null
        }
    }

    fun delete(key: String) {
        sharedPrefs.edit().remove(key).apply()
    }
}
