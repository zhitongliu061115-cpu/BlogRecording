package com.example.blogrecording.importing

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult

enum class LocalMediaKind {
    AUDIO,
    VIDEO
}

data class LocalMediaFileMetadata(
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long?,
    val kind: LocalMediaKind
)

object LocalMediaImportPolicy {
    const val MAX_IMPORT_BYTES: Long = 500L * 1024L * 1024L
    private const val MAX_DISPLAY_NAME_CHARS = 120

    fun validate(
        displayName: String?,
        mimeType: String?,
        sizeBytes: Long?
    ): AppResult<LocalMediaFileMetadata> {
        if (sizeBytes != null && sizeBytes <= 0L) {
            return AppResult.Failure(AppError.LocalMediaReadFailed)
        }
        if (sizeBytes != null && sizeBytes > MAX_IMPORT_BYTES) {
            return AppResult.Failure(AppError.LocalMediaTooLarge)
        }
        val safeName = sanitizeDisplayName(displayName)
        val kind = resolveKind(safeName, mimeType)
            ?: return AppResult.Failure(AppError.LocalMediaUnsupported)
        return AppResult.Success(
            LocalMediaFileMetadata(
                displayName = safeName,
                mimeType = mimeType?.trim()?.takeIf { it.isNotBlank() },
                sizeBytes = sizeBytes,
                kind = kind
            )
        )
    }

    fun sanitizeDisplayName(displayName: String?): String {
        val cleaned = displayName
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.replace(Regex("[\\r\\n\\t]"), " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Imported media"
        return cleaned.take(MAX_DISPLAY_NAME_CHARS)
    }

    private fun resolveKind(displayName: String, mimeType: String?): LocalMediaKind? {
        val normalizedMime = mimeType?.lowercase()?.substringBefore(';')?.trim().orEmpty()
        if (normalizedMime.startsWith("audio/")) return LocalMediaKind.AUDIO
        if (normalizedMime.startsWith("video/")) return LocalMediaKind.VIDEO

        val extension = displayName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
        return when (extension) {
            "mp3", "m4a", "aac", "wav", "flac", "ogg", "opus" -> LocalMediaKind.AUDIO
            "mp4", "mkv", "mov", "webm", "3gp" -> LocalMediaKind.VIDEO
            else -> null
        }
    }
}
