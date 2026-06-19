package com.example.blogrecording.platform

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidManifestContractTest {
    @Test
    fun cosmosPackageIsVisibleForTargetedPlaybackCapture() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("""<queries>"""))
        assertTrue(manifest.contains("""<package android:name="app.podcast.cosmos" />"""))
    }

    @Test
    fun privilegedSystemAudioPermissionsAreDeclaredForSystemSignedBuilds() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("""android.permission.CAPTURE_AUDIO_OUTPUT"""))
        assertTrue(manifest.contains("""android.permission.CAPTURE_MEDIA_OUTPUT"""))
        assertTrue(manifest.contains("""tools:ignore="ProtectedPermissions""""))
    }
}
