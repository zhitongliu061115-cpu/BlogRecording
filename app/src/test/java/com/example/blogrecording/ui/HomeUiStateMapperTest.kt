package com.example.blogrecording.ui

import com.example.blogrecording.data.AudioSourceType
import com.example.blogrecording.data.PodcastSession
import com.example.blogrecording.data.PodcastSessionDetail
import com.example.blogrecording.data.PodcastSessionStatus
import com.example.blogrecording.data.RecordingSegment
import com.example.blogrecording.data.RecordingSegmentStatus
import com.example.blogrecording.data.SessionSummary
import com.example.blogrecording.data.SpeakerProfileEntity
import com.example.blogrecording.data.SummaryLanguage
import com.example.blogrecording.data.SummaryStatus
import com.example.blogrecording.data.SummaryStyle
import com.example.blogrecording.data.TranscriptSegmentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiStateMapperTest {
    @Test
    fun emptyDetailsShowEmptyHomeState() {
        val state = HomeUiStateMapper.map(emptyList())

        assertTrue(state.isEmpty)
        assertTrue(state.cards.isEmpty())
        assertNull(state.activeRecordingSessionId)
    }

    @Test
    fun cardShowsLabelsDurationSegmentsTranscriptAndSummary() {
        val detail = detail(
            session = session(
                id = "session-1",
                title = "Episode A",
                status = PodcastSessionStatus.READY_FOR_SUMMARY,
                transcript = "hello",
                transcriptSegmentCount = 2,
                summary = summary(SummaryStatus.READY)
            ),
            recordingSegments = listOf(
                segment(sessionId = "session-1", id = "segment-1", durationMs = 61_000L),
                segment(sessionId = "session-1", id = "segment-2", durationMs = 2_000L)
            )
        )

        val card = HomeUiStateMapper.map(listOf(detail)).cards.single()

        assertEquals("Episode A", card.title)
        assertEquals("可总结", card.statusLabel)
        assertEquals("1:03", card.durationLabel)
        assertEquals("2 段", card.segmentCountLabel)
        assertEquals("已转写 2 段", card.transcriptionLabel)
        assertEquals("可总结", card.summaryLabel)
    }

    @Test
    fun draftCanStartButCannotPauseResumeFinishOrSummarize() {
        val card = HomeUiStateMapper.map(listOf(detail(session = session(status = PodcastSessionStatus.DRAFT))))
            .cards
            .single()

        assertTrue(card.actionState.canStart)
        assertFalse(card.actionState.canPause)
        assertFalse(card.actionState.canResume)
        assertFalse(card.canFinish)
        assertTrue(card.canRename)
        assertFalse(card.canStartSummary)
        assertEquals("没有可总结的转写", card.startSummaryDisabledReason)
    }

    @Test
    fun recordingCanPauseAndFinishButCannotSummarize() {
        val card = HomeUiStateMapper.map(
            listOf(
                detail(
                    session = session(
                        status = PodcastSessionStatus.RECORDING,
                        activeSegmentId = "segment-1",
                        transcript = "hello"
                    ),
                    recordingSegments = listOf(
                        segment(
                            id = "segment-1",
                            status = RecordingSegmentStatus.RECORDING
                        )
                    )
                )
            )
        ).cards.single()

        assertTrue(card.isRecording)
        assertEquals("录制中", card.statusLabel)
        assertFalse(card.actionState.canStart)
        assertTrue(card.actionState.canPause)
        assertFalse(card.actionState.canResume)
        assertTrue(card.canFinish)
        assertFalse(card.canStartSummary)
        assertEquals("请先暂停当前录音", card.startSummaryDisabledReason)
    }

    @Test
    fun pausedWithTranscriptCanResumeFinishAndSummarize() {
        val card = HomeUiStateMapper.map(
            listOf(
                detail(
                    session = session(
                        status = PodcastSessionStatus.PAUSED,
                        transcript = "hello",
                        transcriptSegmentCount = 1
                    ),
                    recordingSegments = listOf(segment(status = RecordingSegmentStatus.COMPLETED))
                )
            )
        ).cards.single()

        assertFalse(card.actionState.canStart)
        assertFalse(card.actionState.canPause)
        assertTrue(card.actionState.canResume)
        assertTrue(card.canFinish)
        assertTrue(card.canStartSummary)
        assertNull(card.startSummaryDisabledReason)
    }

    @Test
    fun anotherRecordingSessionMarksStartOrResumeAsSwitching() {
        val recording = detail(
            session = session(
                id = "session-a",
                status = PodcastSessionStatus.RECORDING,
                activeSegmentId = "segment-a"
            ),
            recordingSegments = listOf(
                segment(sessionId = "session-a", id = "segment-a", status = RecordingSegmentStatus.RECORDING)
            )
        )
        val paused = detail(
            session = session(
                id = "session-b",
                status = PodcastSessionStatus.PAUSED,
                transcript = "hello"
            ),
            recordingSegments = listOf(
                segment(sessionId = "session-b", id = "segment-b", status = RecordingSegmentStatus.COMPLETED)
            )
        )

        val state = HomeUiStateMapper.map(listOf(recording, paused))
        val pausedCard = state.cards.single { it.sessionId == "session-b" }

        assertEquals("session-a", state.activeRecordingSessionId)
        assertTrue(pausedCard.actionState.canResume)
        assertTrue(pausedCard.actionState.switchingFromAnotherSession)
        assertFalse(pausedCard.canStartSummary)
        assertEquals("请先暂停当前录音", pausedCard.startSummaryDisabledReason)
    }

    @Test
    fun atMostOneCardIsMarkedRecording() {
        val first = detail(
            session = session(
                id = "session-a",
                status = PodcastSessionStatus.RECORDING,
                activeSegmentId = "segment-a"
            ),
            recordingSegments = listOf(
                segment(sessionId = "session-a", id = "segment-a", status = RecordingSegmentStatus.RECORDING)
            )
        )
        val second = detail(
            session = session(
                id = "session-b",
                status = PodcastSessionStatus.RECORDING,
                activeSegmentId = "segment-b"
            ),
            recordingSegments = listOf(
                segment(sessionId = "session-b", id = "segment-b", status = RecordingSegmentStatus.RECORDING)
            )
        )

        val state = HomeUiStateMapper.map(listOf(first, second))

        assertEquals(1, state.cards.count { it.isRecording })
        assertEquals("session-a", state.activeRecordingSessionId)
    }

    private fun detail(
        session: PodcastSession,
        recordingSegments: List<RecordingSegment> = emptyList(),
        transcriptSegments: List<TranscriptSegmentEntity> = emptyList()
    ): PodcastSessionDetail {
        return PodcastSessionDetail(
            session = session,
            recordingSegments = recordingSegments,
            transcriptSegments = transcriptSegments,
            speakerProfiles = emptyList<SpeakerProfileEntity>()
        )
    }

    private fun session(
        id: String = "session-1",
        title: String = "Episode",
        status: PodcastSessionStatus = PodcastSessionStatus.DRAFT,
        activeSegmentId: String? = null,
        transcript: String = "",
        transcriptSegmentCount: Int = 0,
        summary: SessionSummary? = null
    ): PodcastSession {
        return PodcastSession(
            id = id,
            title = title,
            createdAt = 1L,
            updatedAt = 2L,
            sourceType = AudioSourceType.MICROPHONE,
            status = status,
            activeSegmentId = activeSegmentId,
            lastCompletedSegmentId = null,
            transcript = transcript,
            summary = summary,
            summaryStyle = SummaryStyle.POINTS_QUOTES_ACTIONS,
            summaryLanguage = SummaryLanguage.CHINESE,
            summaryModelName = "deepseek-chat",
            asrModelName = "SenseVoice sherpa-onnx",
            vadModelName = "Silero VAD sherpa-onnx",
            diarizationModelName = "sherpa-onnx speaker diarization",
            detectedSpeakerCount = 0,
            recordingSegmentCount = 0,
            transcriptSegmentCount = transcriptSegmentCount,
            errorMessage = null,
            legacyRecordingSessionId = null
        )
    }

    private fun segment(
        sessionId: String = "session-1",
        id: String = "segment-1",
        status: RecordingSegmentStatus = RecordingSegmentStatus.COMPLETED,
        durationMs: Long = 0L
    ): RecordingSegment {
        return RecordingSegment(
            id = id,
            sessionId = sessionId,
            index = 1,
            sourceType = AudioSourceType.MICROPHONE,
            status = status,
            startedAt = 1L,
            endedAt = 2L,
            durationMs = durationMs,
            pcmFilePath = null,
            audioFilePath = null,
            sampleRate = null,
            channelCount = null,
            transcriptSegmentIds = emptyList(),
            errorMessage = null,
            createdAt = 1L,
            updatedAt = 2L
        )
    }

    private fun summary(status: SummaryStatus): SessionSummary {
        return SessionSummary(
            text = "",
            status = status,
            modelName = "deepseek-chat",
            generatedAt = null,
            updatedAt = 1L,
            errorMessage = null
        )
    }
}
