package com.example.blogrecording.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiKeyStoreContractTest {
    @Test
    fun contractKeepsStorageNamesAndCryptoParameters() {
        assertEquals("deep_seek_key_store", ApiKeyStoreContract.PREFERENCES_NAME)
        assertEquals("AndroidKeyStore", ApiKeyStoreContract.ANDROID_KEYSTORE)
        assertEquals("podcast_recap_deep_seek_api_key", ApiKeyStoreContract.KEY_ALIAS)
        assertEquals("AES/GCM/NoPadding", ApiKeyStoreContract.TRANSFORMATION)
        assertEquals(128, ApiKeyStoreContract.GCM_TAG_LENGTH_BITS)
        assertEquals("iv", ApiKeyStoreContract.KEY_IV)
        assertEquals("cipher_text", ApiKeyStoreContract.KEY_CIPHER_TEXT)
    }

    @Test
    fun normalizeApiKeyTrimsAndRejectsBlankValues() {
        assertEquals("sk-test", ApiKeyStoreContract.normalizeApiKey("  sk-test  "))
        assertNull(ApiKeyStoreContract.normalizeApiKey("   "))
    }

    @Test
    fun storedPayloadRequiresBothIvAndCipherText() {
        assertTrue(ApiKeyStoreContract.hasStoredPayload("iv", "cipher"))
        assertFalse(ApiKeyStoreContract.hasStoredPayload(null, "cipher"))
        assertFalse(ApiKeyStoreContract.hasStoredPayload("iv", null))
    }
}
