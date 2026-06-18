package com.example.blogrecording.security

internal object ApiKeyStoreContract {
    const val PREFERENCES_NAME = "deep_seek_key_store"
    const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val KEY_ALIAS = "podcast_recap_deep_seek_api_key"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val GCM_TAG_LENGTH_BITS = 128
    const val KEY_IV = "iv"
    const val KEY_CIPHER_TEXT = "cipher_text"

    fun normalizeApiKey(apiKey: String): String? {
        return apiKey.trim().takeIf { it.isNotBlank() }
    }

    fun hasStoredPayload(iv: String?, cipherText: String?): Boolean {
        return iv != null && cipherText != null
    }
}
