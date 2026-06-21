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
import com.example.blogrecording.ui.state.ProcessingStage
import com.example.blogrecording.ui.state.ProcessingStageUiState
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
    fun recordingCardShowsActiveProcessingStage() {
        val stage = ProcessingStageUiState.buffering(bufferedMs = 12_000L, targetMs = 30_000L)
        val card = HomeUiStateMapper.map(
            details = listOf(
                detail(
                    session = session(
                        id = "session-1",
                        status = PodcastSessionStatus.RECORDING,
                        activeSegmentId = "segment-1"
                    ),
                    recordingSegments = listOf(
                        segment(
                            id = "segment-1",
                            status = RecordingSegmentStatus.RECORDING
                        )
                    )
                )
            ),
            processingStage = stage,
            processingSessionId = "session-1"
        ).cards.single()

        assertEquals(ProcessingStage.BUFFERING, card.processingStage.stage)
        assertEquals("12/30 秒", card.processingStage.progressLabel)
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
    fun importedSessionWithTranscriptCanSummarizeButNotStartRecording() {
        val card = HomeUiStateMapper.map(
            listOf(
                detail(
                    session = session(
                        status = PodcastSessionStatus.READY_FOR_SUMMARY,
                        sourceType = AudioSourceType.LOCAL_MEDIA,
                        transcript = "import transcript",
                        transcriptSegmentCount = 1
                    ),
                    recordingSegments = listOf(
                        segment(
                            sourceType = AudioSourceType.LOCAL_MEDIA,
                            status = RecordingSegmentStatus.COMPLETED
                        )
                    )
                )
            )
        ).cards.single()

        assertFalse(card.actionState.canStart)
        assertFalse(card.actionState.canResume)
        assertTrue(card.canStartSummary)
        assertEquals("可总结", card.statusLabel)
    }

    @Test
    fun failedSummaryCanRetryWhenTranscriptExists() {
        val card = HomeUiStateMapper.map(
            listOf(
                detail(
                    session = session(
                        status = PodcastSessionStatus.READY_FOR_SUMMARY,
                        transcript = "hello",
                        summary = summary(SummaryStatus.FAILED)
                    )
                )
            )
        ).cards.single()

        assertEquals("总结失败", card.summaryLabel)
        assertTrue(card.canStartSummary)
        assertNull(card.startSummaryDisabledReason)
    }

    @Test
    fun transcriptSegmentsMakeSummaryReadyEvenWhenSessionTranscriptIsBlank() {
        val card = HomeUiStateMapper.map(
            listOf(
                detail(
                    session = session(status = PodcastSessionStatus.PAUSED),
                    transcriptSegments = listOf(transcriptSegment(text = "segment transcript"))
                )
            )
        ).cards.single()

        assertEquals("可总结", card.summaryLabel)
        assertTrue(card.canStartSummary)
    }

    @Test
    fun summarizingSummaryCannotStartAgain() {
        val card = HomeUiStateMapper.map(
            listOf(
                detail(
                    session = session(
                        status = PodcastSessionStatus.SUMMARIZING,
                        transcript = "hello",
                        summary = summary(SummaryStatus.SUMMARIZING)
                    )
                )
            )
        ).cards.single()

        assertEquals("总结中", card.summaryLabel)
        assertFalse(card.canStartSummary)
        assertEquals("当前状态不可总结", card.startSummaryDisabledReason)
    }

    @Test
    fun summaryStartRequiresApiKeyWhenTranscriptExists() {
        val card = HomeUiStateMapper.map(
            details = listOf(
                detail(
                    session = session(
                        status = PodcastSessionStatus.PAUSED,
                        transcript = "hello"
                    ),
                    recordingSegments = listOf(segment(status = RecordingSegmentStatus.COMPLETED))
                )
            ),
            hasApiKey = false
        ).cards.single()

        assertFalse(card.canStartSummary)
        assertEquals("请先配置 DeepSeek API Key", card.startSummaryDisabledReason)
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

    @Test
    fun homeShowsOnlyLatestFiveCardsByUpdatedAt() {
        val details = (1..7).map { index ->
            detail(
                session = session(
                    id = "session-$index",
                    title = "Episode $index",
                    updatedAt = index.toLong()
                )
            )
        }

        val state = HomeUiStateMapper.map(details)

        assertEquals(5, state.cards.size)
        assertEquals(
            listOf("session-7", "session-6", "session-5", "session-4", "session-3"),
            state.cards.map { it.sessionId }
        )
    }

    @Test
    fun cardShowsRecentTranscriptPreviewSnippets() {
        val card = HomeUiStateMapper.map(
            listOf(
                detail(
                    session = session(
                        id = "session-1",
                        transcript = "aggregate",
                        transcriptSegmentCount = 4
                    ),
                    transcriptSegments = listOf(
                        transcriptSegment(text = "first", startMs = 1_000L),
                        transcriptSegment(text = "second", startMs = 2_000L),
                        transcriptSegment(text = " ", startMs = 3_000L),
                        transcriptSegment(text = "third", startMs = 4_000L),
                        transcriptSegment(text = "fourth", startMs = 5_000L)
                    )
                )
            )
        ).cards.single()

        assertEquals(
            listOf("second", "third", "fourth"),
            card.transcriptPreviewSnippets.map { it.text }
        )
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
        updatedAt: Long = 2L,
        transcript: String = "",
        transcriptSegmentCount: Int = 0,
        summary: SessionSummary? = null
        ,
        sourceType: AudioSourceType = AudioSourceType.MICROPHONE
    ): PodcastSession {
        return PodcastSession(
            id = id,
            title = title,
            createdAt = 1L,
            updatedAt = updatedAt,
            sourceType = sourceType,
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
        durationMs: Long = 0L,
        sourceType: AudioSourceType = AudioSourceType.MICROPHONE
    ): RecordingSegment {
        return RecordingSegment(
            id = id,
            sessionId = sessionId,
            index = 1,
            sourceType = sourceType,
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

    private fun transcriptSegment(
        sessionId: String = "session-1",
        startMs: Long = 1L,
        text: String
    ): TranscriptSegmentEntity {
        return TranscriptSegmentEntity(
            id = "transcript-1",
            sessionId = sessionId,
            recordingSegmentId = null,
            startMs = startMs,
            endMs = startMs + 1L,
            speakerId = "speaker-1",
            speakerDisplayName = "Speaker 1",
            text = text,
            language = "zh",
            confidence = null,
            vadConfidence = null,
            isFinal = true,
            createdAt = startMs
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
