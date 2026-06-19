package com.example.blogrecording.audio

import android.media.AudioAttributes
import org.junit.Assert.assertTrue
import org.junit.Test

class InternalAudioCapturePolicyTest {
    @Test
    fun systemAudioCaptureIncludesGameMediaAndUnknownUsages() {
        assertTrue(InternalAudioCapturePolicy.matchingUsages.contains(AudioAttributes.USAGE_MEDIA))
        assertTrue(InternalAudioCapturePolicy.matchingUsages.contains(AudioAttributes.USAGE_GAME))
        assertTrue(InternalAudioCapturePolicy.matchingUsages.contains(AudioAttributes.USAGE_UNKNOWN))
    }

    @Test
    fun preferredCapturePackagesIncludeCosmosPodcast() {
        assertTrue(
            InternalAudioCapturePolicy.preferredCapturePackages.contains(
                InternalAudioCapturePolicy.COSMOS_PACKAGE_NAME
            )
        )
    }
}
