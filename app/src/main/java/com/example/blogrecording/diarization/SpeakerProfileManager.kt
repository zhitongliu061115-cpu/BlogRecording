package com.example.blogrecording.diarization

import com.example.blogrecording.data.SpeakerProfileEntity
import com.example.blogrecording.data.TranscriptSegmentEntity
import java.util.UUID

class SpeakerProfileManager {
    fun buildProfiles(sessionId: String, segments: List<TranscriptSegmentEntity>): List<SpeakerProfileEntity> {
        val now = System.currentTimeMillis()
        return segments.groupBy { it.speakerId }.entries.mapIndexed { index, entry ->
            SpeakerProfileEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                speakerId = entry.key,
                displayName = entry.value.firstOrNull()?.speakerDisplayName ?: "未知说话人",
                colorIndex = index,
                segmentCount = entry.value.size,
                totalSpeechDurationMs = entry.value.sumOf { (it.endMs - it.startMs).coerceAtLeast(0L) },
                createdAt = now,
                updatedAt = now
            )
        }
    }
}
