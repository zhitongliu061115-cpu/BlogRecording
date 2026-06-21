package com.example.blogrecording.data

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PodcastSessionJsonCodecTest {
    @Test
    fun podcastSessionRoundTripsThroughJson() {
        val session = podcastSession()

        val decoded = PodcastSessionJsonCodec.decodeSession(
            PodcastSessionJsonCodec.encodeSession(session)
        )

        assertEquals(session, decoded)
    }

    @Test
    fun podcastSessionDecodesMissingOptionalFields() {
        val json = PodcastSessionJsonCodec.encodeSession(
            podcastSession(sourceType = null, summary = null, legacyRecordingSessionId = null)
        )

        val decoded = PodcastSessionJsonCodec.decodeSession(json)

        assertNull(decoded.sourceType)
        assertNull(decoded.summary)
        assertNull(decoded.activeSegmentId)
        assertNull(decoded.legacyRecordingSessionId)
    }

    @Test
    fun recordingSegmentsRoundTripAndSortByIndex() {
        val second = recordingSegment(id = "segment-2", index = 2)
        val first = recordingSegment(id = "segment-1", index = 1)
        val encoded = JSONArray(listOf(
            PodcastSessionJsonCodec.encodeRecordingSegment(second),
            PodcastSessionJsonCodec.encodeRecordingSegment(first)
        ))

        val decoded = PodcastSessionJsonCodec.decodeRecordingSegments(encoded)

        assertEquals(listOf(first, second), decoded)
    }

    @Test
    fun recordingSegmentStatusesPersistThroughJson() {
        RecordingSegmentStatus.entries.forEach { status ->
            val segment = recordingSegment(
                id = "segment-${status.name.lowercase()}",
                index = status.ordinal + 1
            ).copy(
                status = status,
                errorMessage = if (status == RecordingSegmentStatus.ERROR) "failed" else null
            )

            val decoded = PodcastSessionJsonCodec.decodeRecordingSegment(
                PodcastSessionJsonCodec.encodeRecordingSegment(segment)
            )

            assertEquals(status, decoded.status)
            assertEquals(segment.errorMessage, decoded.errorMessage)
        }
    }

    @Test
    fun summaryRoundTripsThroughJson() {
        val summary = SessionSummary(
            text = "summary",
            status = SummaryStatus.SUMMARIZED,
            modelName = "deepseek-chat",
            generatedAt = 1_000L,
            updatedAt = 2_000L,
            errorMessage = null
        )

        val decoded = PodcastSessionJsonCodec.decodeSummary(
            PodcastSessionJsonCodec.encodeSummary(summary)
        )

        assertEquals(summary, decoded)
    }

    @Test
    fun importedContentRoundTripsThroughPodcastSessionJson() {
        val metadata = ImportedContentMetadata(
            kind = ImportedContentKind.LOCAL_MEDIA,
            displayName = "episode.mp3",
            mimeType = "audio/mpeg",
            sizeBytes = 123_456L,
            durationMs = 60_000L,
            status = ImportedContentStatus.COMPLETED,
            errorMessage = null,
            importedAt = 1_000L,
            updatedAt = 2_000L
        )
        val session = podcastSession(sourceType = AudioSourceType.LOCAL_MEDIA).copy(
            importedContent = metadata
        )

        val decoded = PodcastSessionJsonCodec.decodeSession(
            PodcastSessionJsonCodec.encodeSession(session)
        )

        assertEquals(AudioSourceType.LOCAL_MEDIA, decoded.sourceType)
        assertEquals(metadata, decoded.importedContent)
    }

    @Test
    fun legacyPodcastSessionJsonDefaultsMissingImportedContent() {
        val json = PodcastSessionJsonCodec.encodeSession(podcastSession())
        json.remove("importedContent")

        val decoded = PodcastSessionJsonCodec.decodeSession(json)

        assertNull(decoded.importedContent)
    }

    private fun podcastSession(
        sourceType: AudioSourceType? = AudioSourceType.MICROPHONE,
        summary: SessionSummary? = SessionSummary(
            text = "summary",
            status = SummaryStatus.SUMMARIZED,
            modelName = "deepseek-chat",
            generatedAt = 1_000L,
            updatedAt = 2_000L,
            errorMessage = null
        ),
        legacyRecordingSessionId: String? = "legacy-1"
    ): PodcastSession {
        return PodcastSession(
            id = "session-1",
            title = "Episode",
            createdAt = 100L,
            updatedAt = 200L,
            sourceType = sourceType,
            status = PodcastSessionStatus.SUMMARIZED,
            activeSegmentId = null,
            lastCompletedSegmentId = "segment-2",
            transcript = "transcript",
            summary = summary,
            summaryStyle = SummaryStyle.POINTS_QUOTES_ACTIONS,
            summaryLanguage = SummaryLanguage.CHINESE,
            summaryModelName = "deepseek-chat",
            asrModelName = "SenseVoice sherpa-onnx",
            vadModelName = "Silero VAD sherpa-onnx",
            diarizationModelName = "sherpa-onnx speaker diarization",
            detectedSpeakerCount = 2,
            recordingSegmentCount = 2,
            transcriptSegmentCount = 3,
            errorMessage = null,
            legacyRecordingSessionId = legacyRecordingSessionId
        )
    }

    private fun recordingSegment(id: String, index: Int): RecordingSegment {
        return RecordingSegment(
            id = id,
            sessionId = "session-1",
            index = index,
            sourceType = AudioSourceType.MICROPHONE,
            status = RecordingSegmentStatus.COMPLETED,
            startedAt = index * 1_000L,
            endedAt = index * 1_000L + 500L,
            durationMs = 500L,
            pcmFilePath = null,
            audioFilePath = null,
            sampleRate = 16_000,
            channelCount = 1,
            transcriptSegmentIds = listOf("transcript-$index"),
            errorMessage = null,
            createdAt = index * 1_000L,
            updatedAt = index * 1_000L + 500L
        )
    }
}
