package com.example.blogrecording.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingPersistenceContractTest {
    @Test
    fun contractKeepsDatastoreAndKeyFamilies() {
        assertEquals("podcast_recap_records", RecordingPersistenceContract.DATASTORE_NAME)
        assertEquals("schema_version", RecordingPersistenceContract.SCHEMA_VERSION_KEY)
        assertEquals(3, RecordingPersistenceContract.CURRENT_SCHEMA_VERSION)
        assertEquals("session_order", RecordingPersistenceContract.SESSION_ORDER_KEY)
        assertEquals("session_abc", RecordingPersistenceContract.sessionKey("abc"))
        assertEquals("segments_abc", RecordingPersistenceContract.segmentsKey("abc"))
        assertEquals("speakers_abc", RecordingPersistenceContract.speakersKey("abc"))
        assertEquals("recording_segments_abc", RecordingPersistenceContract.recordingSegmentsKey("abc"))
    }

    @Test
    fun contractKeepsSessionJsonFieldsAndEnumNames() {
        assertEquals(17, RecordingPersistenceContract.SESSION_FIELDS.size)
        assertTrue("sourceType" in RecordingPersistenceContract.SESSION_FIELDS)
        assertTrue("summaryLanguage" in RecordingPersistenceContract.SESSION_FIELDS)
        assertEquals("INTERNAL_AUDIO", AudioSourceType.INTERNAL_AUDIO.name)
        assertEquals("MICROPHONE", AudioSourceType.MICROPHONE.name)
        assertEquals("LOCAL_MEDIA", AudioSourceType.LOCAL_MEDIA.name)
        assertEquals("CAPTURING_AUDIO", RecordingStatus.CAPTURING_AUDIO.name)
        assertEquals("VAD_DETECTING", RecordingStatus.VAD_DETECTING.name)
        assertEquals("DIARIZING", RecordingStatus.DIARIZING.name)
        assertEquals("TRANSCRIBING", RecordingStatus.TRANSCRIBING.name)
        assertEquals("SUMMARIZING", RecordingStatus.SUMMARIZING.name)
    }

    @Test
    fun contractKeepsSegmentAndSpeakerProfileJsonFields() {
        assertEquals(13, RecordingPersistenceContract.SEGMENT_FIELDS.size)
        assertTrue("recordingSegmentId" in RecordingPersistenceContract.SEGMENT_FIELDS)
        assertTrue("speakerDisplayName" in RecordingPersistenceContract.SEGMENT_FIELDS)
        assertTrue("vadConfidence" in RecordingPersistenceContract.SEGMENT_FIELDS)

        assertEquals(9, RecordingPersistenceContract.SPEAKER_PROFILE_FIELDS.size)
        assertTrue("totalSpeechDurationMs" in RecordingPersistenceContract.SPEAKER_PROFILE_FIELDS)
        assertTrue("colorIndex" in RecordingPersistenceContract.SPEAKER_PROFILE_FIELDS)
    }

    @Test
    fun contractDefinesPodcastSessionAndRecordingSegmentJsonFields() {
        assertEquals(22, RecordingPersistenceContract.PODCAST_SESSION_FIELDS.size)
        assertTrue("activeSegmentId" in RecordingPersistenceContract.PODCAST_SESSION_FIELDS)
        assertTrue("legacyRecordingSessionId" in RecordingPersistenceContract.PODCAST_SESSION_FIELDS)
        assertTrue("summary" in RecordingPersistenceContract.PODCAST_SESSION_FIELDS)
        assertTrue("importedContent" in RecordingPersistenceContract.PODCAST_SESSION_FIELDS)

        assertEquals(9, RecordingPersistenceContract.IMPORTED_CONTENT_FIELDS.size)
        assertTrue("displayName" in RecordingPersistenceContract.IMPORTED_CONTENT_FIELDS)
        assertTrue("mimeType" in RecordingPersistenceContract.IMPORTED_CONTENT_FIELDS)
        assertTrue("status" in RecordingPersistenceContract.IMPORTED_CONTENT_FIELDS)

        assertEquals(6, RecordingPersistenceContract.SESSION_SUMMARY_FIELDS.size)
        assertTrue("generatedAt" in RecordingPersistenceContract.SESSION_SUMMARY_FIELDS)
        assertTrue("errorMessage" in RecordingPersistenceContract.SESSION_SUMMARY_FIELDS)

        assertEquals(16, RecordingPersistenceContract.RECORDING_SEGMENT_FIELDS.size)
        assertTrue("transcriptSegmentIds" in RecordingPersistenceContract.RECORDING_SEGMENT_FIELDS)
        assertTrue("pcmFilePath" in RecordingPersistenceContract.RECORDING_SEGMENT_FIELDS)
        assertTrue("audioFilePath" in RecordingPersistenceContract.RECORDING_SEGMENT_FIELDS)
    }
}
