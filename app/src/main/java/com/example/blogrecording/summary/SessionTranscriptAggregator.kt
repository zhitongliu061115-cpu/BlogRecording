package com.example.blogrecording.summary

import com.example.blogrecording.data.PodcastSessionDetail
import com.example.blogrecording.data.TranscriptSegmentEntity
import com.example.blogrecording.data.toTranscriptText

object SessionTranscriptAggregator {
    fun aggregate(detail: PodcastSessionDetail): String {
        val sessionId = detail.session.id
        val transcripts = detail.transcriptSegments
            .filter { it.sessionId == sessionId }
            .filter { it.text.isNotBlank() }

        val includedIds = mutableSetOf<String>()
        val ordered = mutableListOf<TranscriptSegmentEntity>()

        detail.recordingSegments
            .filter { it.sessionId == sessionId }
            .sortedBy { it.index }
            .forEach { segment ->
                transcripts
                    .filter { it.recordingSegmentId == segment.id }
                    .sortedBy { it.startMs }
                    .forEach { transcript ->
                        if (includedIds.add(transcript.id)) {
                            ordered += transcript
                        }
                    }
            }

        transcripts
            .filter { it.recordingSegmentId == null }
            .sortedBy { it.startMs }
            .forEach { transcript ->
                if (includedIds.add(transcript.id)) {
                    ordered += transcript
                }
            }

        val segmentTranscript = ordered.joinToString(separator = "\n\n") { it.toTranscriptText() }
        return segmentTranscript.ifBlank { detail.session.transcript.trim() }
    }
}
