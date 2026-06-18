package com.example.blogrecording.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PodcastSessionStateMachineTest {
    @Test
    fun createReturnsDraftWithoutActiveSegment() {
        val created = PodcastSessionStateMachine.create(session(status = PodcastSessionStatus.ERROR))

        assertEquals(PodcastSessionStatus.DRAFT, created.status)
        assertNull(created.activeSegmentId)
        assertNull(created.errorMessage)
    }

    @Test
    fun startAndPausePreserveSessionIdentityAndSegmentMembership() {
        val draft = session(status = PodcastSessionStatus.DRAFT, recordingSegmentCount = 1)

        val recording = PodcastSessionStateMachine.start(draft, segmentId = "segment-1")
        val paused = PodcastSessionStateMachine.pause(recording)

        assertEquals("session-1", paused.id)
        assertEquals(PodcastSessionStatus.PAUSED, paused.status)
        assertNull(paused.activeSegmentId)
        assertEquals("segment-1", paused.lastCompletedSegmentId)
        assertEquals(1, paused.recordingSegmentCount)
    }

    @Test
    fun resumeUsesSameRulesAsStart() {
        val paused = session(status = PodcastSessionStatus.PAUSED)

        val recording = PodcastSessionStateMachine.resume(paused, segmentId = "segment-2")

        assertEquals(PodcastSessionStatus.RECORDING, recording.status)
        assertEquals("segment-2", recording.activeSegmentId)
    }

    @Test
    fun invalidStartStateThrows() {
        val summarizing = session(status = PodcastSessionStatus.SUMMARIZING)

        assertThrows(IllegalArgumentException::class.java) {
            PodcastSessionStateMachine.start(summarizing, segmentId = "segment-1")
        }
    }

    @Test
    fun finalizeMarksPausedSessionReadyForSummary() {
        val paused = session(status = PodcastSessionStatus.PAUSED)

        val ready = PodcastSessionStateMachine.finalize(paused)

        assertEquals(PodcastSessionStatus.READY_FOR_SUMMARY, ready.status)
        assertNull(ready.activeSegmentId)
    }

    @Test
    fun processingRequiresRecordingOrPausedState() {
        val recording = session(status = PodcastSessionStatus.RECORDING)

        val processing = PodcastSessionStateMachine.startTranscription(recording)

        assertEquals(PodcastSessionStatus.PROCESSING, processing.status)
        assertThrows(IllegalArgumentException::class.java) {
            PodcastSessionStateMachine.startTranscription(session(status = PodcastSessionStatus.SUMMARIZED))
        }
    }

    @Test
    fun readyForSummaryRequiresTranscript() {
        val processing = session(status = PodcastSessionStatus.PROCESSING, transcript = "finished text")

        val ready = PodcastSessionStateMachine.markReadyForSummary(processing)

        assertEquals(PodcastSessionStatus.READY_FOR_SUMMARY, ready.status)
        assertThrows(IllegalArgumentException::class.java) {
            PodcastSessionStateMachine.markReadyForSummary(processing.copy(transcript = " "))
        }
    }

    @Test
    fun summaryLifecycleMovesReadyToSummarized() {
        val ready = session(status = PodcastSessionStatus.READY_FOR_SUMMARY)
        val summarizing = PodcastSessionStateMachine.summarize(ready)
        val summary = SessionSummary(
            text = "summary",
            status = SummaryStatus.SUMMARIZING,
            modelName = "deepseek-chat",
            generatedAt = 10L,
            updatedAt = 10L,
            errorMessage = null
        )

        val summarized = PodcastSessionStateMachine.markSummarized(summarizing, summary)

        assertEquals(PodcastSessionStatus.SUMMARIZED, summarized.status)
        assertEquals(SummaryStatus.SUMMARIZED, summarized.summary?.status)
        assertEquals("summary", summarized.summary?.text)
    }

    @Test
    fun failClearsActiveSegmentAndPreservesExistingData() {
        val recording = session(
            status = PodcastSessionStatus.RECORDING,
            activeSegmentId = "segment-1",
            transcript = "already saved",
            recordingSegmentCount = 1
        )

        val failed = PodcastSessionStateMachine.fail(recording, "capture failed")

        assertEquals(PodcastSessionStatus.ERROR, failed.status)
        assertNull(failed.activeSegmentId)
        assertEquals("already saved", failed.transcript)
        assertEquals(1, failed.recordingSegmentCount)
        assertEquals("capture failed", failed.errorMessage)
    }

    @Test
    fun recoverClearsActiveRecordingAndIsIdempotent() {
        val recording = session(
            status = PodcastSessionStatus.RECORDING,
            activeSegmentId = "segment-1",
            recordingSegmentCount = 1
        )

        val recovered = PodcastSessionStateMachine.recover(recording)
        val recoveredAgain = PodcastSessionStateMachine.recover(recovered)

        assertEquals(PodcastSessionStatus.PAUSED, recovered.status)
        assertNull(recovered.activeSegmentId)
        assertEquals(recovered, recoveredAgain)
    }

    private fun session(
        status: PodcastSessionStatus,
        activeSegmentId: String? = null,
        transcript: String = "",
        recordingSegmentCount: Int = 0
    ): PodcastSession {
        return PodcastSession(
            id = "session-1",
            title = "Episode",
            createdAt = 1L,
            updatedAt = 2L,
            sourceType = AudioSourceType.MICROPHONE,
            status = status,
            activeSegmentId = activeSegmentId,
            lastCompletedSegmentId = null,
            transcript = transcript,
            summary = null,
            summaryStyle = SummaryStyle.POINTS_QUOTES_ACTIONS,
            summaryLanguage = SummaryLanguage.CHINESE,
            summaryModelName = "deepseek-chat",
            asrModelName = "SenseVoice sherpa-onnx",
            vadModelName = "Silero VAD sherpa-onnx",
            diarizationModelName = "sherpa-onnx speaker diarization",
            detectedSpeakerCount = 0,
            recordingSegmentCount = recordingSegmentCount,
            transcriptSegmentCount = 0,
            errorMessage = null,
            legacyRecordingSessionId = null
        )
    }
}
