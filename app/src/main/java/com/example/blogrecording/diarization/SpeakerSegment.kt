package com.example.blogrecording.diarization

data class SpeakerSegment(
    val speakerId: String,
    val displayName: String,
    val unstable: Boolean,
    val vadConfidence: Float?
)
