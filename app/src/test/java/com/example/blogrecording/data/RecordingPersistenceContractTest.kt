package com.example.blogrecording.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingPersistenceContractTest {
    @Test
    fun contractKeepsDatastoreAndKeyFamilies() {
        assertEquals("podcast_recap_records", RecordingPersistenceContract.DATASTORE_NAME)
        assertEquals("session_order", RecordingPersistenceContract.SESSION_ORDER_KEY)
        assertEquals("session_abc", RecordingPersistenceContract.sessionKey("abc"))
        assertEquals("segments_abc", RecordingPersistenceContract.segmentsKey("abc"))
        assertEquals("speakers_abc", RecordingPersistenceContract.speakersKey("abc"))
    }

    @Test
    fun contractKeepsSessionJsonFieldsAndEnumNames() {
        assertEquals(17, RecordingPersistenceContract.SESSION_FIELDS.size)
        assertTrue("sourceType" in RecordingPersistenceContract.SESSION_FIELDS)
        assertTrue("summaryLanguage" in RecordingPersistenceContract.SESSION_FIELDS)
        assertEquals("INTERNAL_AUDIO", AudioSourceType.INTERNAL_AUDIO.name)
        assertEquals("MICROPHONE", AudioSourceType.MICROPHONE.name)
        assertEquals("CAPTURING_AUDIO", RecordingStatus.CAPTURING_AUDIO.name)
        assertEquals("VAD_DETECTING", RecordingStatus.VAD_DETECTING.name)
        assertEquals("DIARIZING", RecordingStatus.DIARIZING.name)
        assertEquals("TRANSCRIBING", RecordingStatus.TRANSCRIBING.name)
        assertEquals("SUMMARIZING", RecordingStatus.SUMMARIZING.name)
    }

    @Test
    fun contractKeepsSegmentAndSpeakerProfileJsonFields() {
        assertEquals(12, RecordingPersistenceContract.SEGMENT_FIELDS.size)
        assertTrue("speakerDisplayName" in RecordingPersistenceContract.SEGMENT_FIELDS)
        assertTrue("vadConfidence" in RecordingPersistenceContract.SEGMENT_FIELDS)

        assertEquals(9, RecordingPersistenceContract.SPEAKER_PROFILE_FIELDS.size)
        assertTrue("totalSpeechDurationMs" in RecordingPersistenceContract.SPEAKER_PROFILE_FIELDS)
        assertTrue("colorIndex" in RecordingPersistenceContract.SPEAKER_PROFILE_FIELDS)
    }
}
