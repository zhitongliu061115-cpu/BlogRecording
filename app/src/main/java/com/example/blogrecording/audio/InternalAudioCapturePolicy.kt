package com.example.blogrecording.audio

import android.media.AudioAttributes

internal object InternalAudioCapturePolicy {
    const val COSMOS_PACKAGE_NAME = "app.podcast.cosmos"

    val matchingUsages = listOf(
        AudioAttributes.USAGE_MEDIA,
        AudioAttributes.USAGE_GAME,
        AudioAttributes.USAGE_UNKNOWN
    )

    val preferredCapturePackages = listOf(COSMOS_PACKAGE_NAME)
}
