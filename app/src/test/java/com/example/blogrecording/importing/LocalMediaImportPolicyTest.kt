package com.example.blogrecording.importing

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMediaImportPolicyTest {
    @Test
    fun acceptsAudioMimeType() {
        val result = LocalMediaImportPolicy.validate(
            displayName = "episode.mp3",
            mimeType = "audio/mpeg",
            sizeBytes = 1_000L
        )

        require(result is AppResult.Success)
        assertEquals(LocalMediaKind.AUDIO, result.value.kind)
        assertEquals("episode.mp3", result.value.displayName)
    }

    @Test
    fun acceptsVideoExtensionWhenMimeTypeIsGeneric() {
        val result = LocalMediaImportPolicy.validate(
            displayName = "clip.mp4",
            mimeType = "application/octet-stream",
            sizeBytes = 1_000L
        )

        require(result is AppResult.Success)
        assertEquals(LocalMediaKind.VIDEO, result.value.kind)
    }

    @Test
    fun rejectsUnsupportedMedia() {
        val result = LocalMediaImportPolicy.validate(
            displayName = "notes.txt",
            mimeType = "text/plain",
            sizeBytes = 1_000L
        )

        assertTrue(result is AppResult.Failure)
        assertEquals(AppError.LocalMediaUnsupported, (result as AppResult.Failure).error)
    }

    @Test
    fun rejectsTooLargeMedia() {
        val result = LocalMediaImportPolicy.validate(
            displayName = "huge.mp3",
            mimeType = "audio/mpeg",
            sizeBytes = LocalMediaImportPolicy.MAX_IMPORT_BYTES + 1L
        )

        assertTrue(result is AppResult.Failure)
        assertEquals(AppError.LocalMediaTooLarge, (result as AppResult.Failure).error)
    }

    @Test
    fun sanitizesDisplayNameWithoutKeepingPaths() {
        val name = LocalMediaImportPolicy.sanitizeDisplayName("C:\\Users\\me\\secret\\episode.mp3")

        assertEquals("episode.mp3", name)
    }
}
