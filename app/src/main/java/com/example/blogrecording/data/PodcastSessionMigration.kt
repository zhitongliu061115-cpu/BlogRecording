package com.example.blogrecording.data

internal object PodcastSessionMigration {
    fun fromLegacyRecordingSession(session: RecordingSessionEntity): PodcastSession {
        val summary = session.summary?.takeIf { it.isNotBlank() }?.let {
            SessionSummary(
                text = it,
                status = SummaryStatus.SUMMARIZED,
                modelName = session.summaryModelName.ifBlank { "deepseek-chat" },
                generatedAt = null,
                updatedAt = session.updatedAt,
                errorMessage = null
            )
        }
        return PodcastSession(
            id = session.id,
            title = session.title,
            createdAt = session.createdAt,
            updatedAt = session.updatedAt,
            sourceType = session.sourceType,
            status = mapLegacyStatus(session),
            activeSegmentId = null,
            lastCompletedSegmentId = null,
            transcript = session.transcript,
            summary = summary,
            summaryStyle = session.summaryStyle,
            summaryLanguage = session.summaryLanguage,
            summaryModelName = session.summaryModelName.ifBlank { "deepseek-chat" },
            asrModelName = session.asrModelName,
            vadModelName = session.vadModelName,
            diarizationModelName = session.diarizationModelName,
            detectedSpeakerCount = session.detectedSpeakerCount,
            recordingSegmentCount = if (session.segmentCount > 0 || session.transcript.isNotBlank()) 1 else 0,
            transcriptSegmentCount = session.segmentCount,
            errorMessage = session.errorMessage,
            legacyRecordingSessionId = session.id
        )
    }

    fun recoverInterrupted(
        session: PodcastSession,
        recordingSegments: List<RecordingSegment>
    ): PodcastSessionDetail {
        val recoveredSession = PodcastSessionStateMachine.recover(session)
        val interruptedSegmentId = session.activeSegmentId
        val recoveredSegments = recordingSegments.map { segment ->
            if (segment.id == interruptedSegmentId && segment.status == RecordingSegmentStatus.RECORDING) {
                segment.copy(
                    status = RecordingSegmentStatus.INTERRUPTED,
                    endedAt = segment.endedAt,
                    updatedAt = recoveredSession.updatedAt,
                    errorMessage = segment.errorMessage ?: "Interrupted during process restart"
                )
            } else {
                segment
            }
        }
        return PodcastSessionDetail(
            session = recoveredSession,
            recordingSegments = recoveredSegments,
            transcriptSegments = emptyList(),
            speakerProfiles = emptyList()
        )
    }

    private fun mapLegacyStatus(session: RecordingSessionEntity): PodcastSessionStatus {
        return when (session.status) {
            RecordingStatus.NOT_STARTED -> PodcastSessionStatus.DRAFT
            RecordingStatus.CAPTURING_AUDIO,
            RecordingStatus.VAD_DETECTING,
            RecordingStatus.DIARIZING,
            RecordingStatus.TRANSCRIBING,
            RecordingStatus.SUMMARIZING -> PodcastSessionStatus.ERROR
            RecordingStatus.COMPLETED -> when {
                !session.summary.isNullOrBlank() -> PodcastSessionStatus.SUMMARIZED
                session.transcript.isNotBlank() -> PodcastSessionStatus.READY_FOR_SUMMARY
                else -> PodcastSessionStatus.PAUSED
            }
            RecordingStatus.ERROR -> PodcastSessionStatus.ERROR
        }
    }
}
