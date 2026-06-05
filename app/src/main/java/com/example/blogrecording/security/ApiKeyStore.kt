package com.example.blogrecording.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ApiKeyStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("deep_seek_key_store", Context.MODE_PRIVATE)

    suspend fun saveApiKey(apiKey: String): AppResult<Unit> {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) {
            return AppResult.Failure(AppError.DeepSeekApiKeyMissing)
        }
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey(), SecureRandom())
            val encrypted = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
            prefs.edit()
                .putString(KEY_IV, Base64.getEncoder().encodeToString(cipher.iv))
                .putString(KEY_CIPHER_TEXT, Base64.getEncoder().encodeToString(encrypted))
                .apply()
            AppResult.Success(Unit)
        }.getOrElse { AppResult.Failure(AppError.Unknown("API Key 加密保存失败")) }
    }

    suspend fun readApiKey(): AppResult<String> {
        val iv = prefs.getString(KEY_IV, null) ?: return AppResult.Failure(AppError.DeepSeekApiKeyMissing)
        val cipherText = prefs.getString(KEY_CIPHER_TEXT, null) ?: return AppResult.Failure(AppError.DeepSeekApiKeyMissing)
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.getDecoder().decode(iv))
            )
            val decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText))
            AppResult.Success(String(decrypted, Charsets.UTF_8))
        }.getOrElse { AppResult.Failure(AppError.DeepSeekApiKeyMissing) }
    }

    fun hasApiKey(): Boolean {
        return prefs.contains(KEY_IV) && prefs.contains(KEY_CIPHER_TEXT)
    }

    fun deleteApiKey() {
        prefs.edit().remove(KEY_IV).remove(KEY_CIPHER_TEXT).apply()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "podcast_recap_deep_seek_api_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
        const val KEY_IV = "iv"
        const val KEY_CIPHER_TEXT = "cipher_text"
    }
}
