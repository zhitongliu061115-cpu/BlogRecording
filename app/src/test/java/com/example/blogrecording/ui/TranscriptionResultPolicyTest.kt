package com.example.blogrecording.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptionResultPolicyTest {
    @Test
    fun weakSingleTokenHallucinationsAreNotPersisted() {
        assertFalse(TranscriptionResultPolicy.shouldPersist("我."))
        assertFalse(TranscriptionResultPolicy.shouldPersist("嗯。"))
        assertFalse(TranscriptionResultPolicy.shouldPersist("uh"))
    }

    @Test
    fun meaningfulShortSpeechCanStillBePersisted() {
        assertTrue(TranscriptionResultPolicy.shouldPersist("你好"))
        assertTrue(TranscriptionResultPolicy.shouldPersist("OK"))
        assertTrue(TranscriptionResultPolicy.shouldPersist("开始讨论"))
    }
}
