package com.example.blogrecording.diarization

class SpeakerLabeler {
    fun displayNameFor(speakerId: String, overrides: Map<String, String>): String {
        return overrides[speakerId] ?: when {
            speakerId.startsWith("speaker_") -> "Speaker ${speakerId.removePrefix("speaker_")}"
            else -> "未知说话人"
        }
    }
}
