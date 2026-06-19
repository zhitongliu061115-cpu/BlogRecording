package com.example.blogrecording.audio

import android.media.AudioAttributes

internal object InternalAudioCapturePolicy {
    val matchingUsages = listOf(
        AudioAttributes.USAGE_MEDIA,
        AudioAttributes.USAGE_GAME,
        AudioAttributes.USAGE_UNKNOWN
    )
}
