package com.example.blogrecording.importing

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import java.net.URI

enum class UrlImportSourceKind {
    XIAOYUZHOU_EPISODE,
    DIRECT_MEDIA,
    RSS_ENCLOSURE,
    UNSUPPORTED
}

data class UrlImportValidation(
    val originalUrl: String,
    val sanitizedUrl: String,
    val host: String,
    val displayName: String,
    val kind: UrlImportSourceKind
)

object UrlMediaImportPolicy {
    const val MAX_DOWNLOAD_BYTES: Long = LocalMediaImportPolicy.MAX_IMPORT_BYTES
    private const val MAX_DISPLAY_NAME_CHARS = 120

    fun validate(input: String): AppResult<UrlImportValidation> {
        val uri = parseHttpUri(input) ?: return AppResult.Failure(AppError.UrlImportInvalidUrl)
        val kind = classify(uri)
        if (kind == UrlImportSourceKind.UNSUPPORTED) {
            return AppResult.Failure(AppError.UrlImportUnsupported)
        }
        return AppResult.Success(
            UrlImportValidation(
                originalUrl = uri.toString(),
                sanitizedUrl = sanitizeUrl(uri),
                host = uri.host.lowercase(),
                displayName = displayNameFor(uri, kind),
                kind = kind
            )
        )
    }

    fun classify(input: String): UrlImportSourceKind {
        val uri = parseHttpUri(input) ?: return UrlImportSourceKind.UNSUPPORTED
        return classify(uri)
    }

    fun classifyResponse(statusCode: Int, contentLength: Long?): AppError? {
        if (statusCode == 401 || statusCode == 403) return AppError.UrlImportUnauthorized
        if (statusCode == 429) return AppError.UrlImportRateLimited
        if (statusCode !in 200..299) return AppError.UrlImportHttpFailed(statusCode)
        if (contentLength != null && contentLength == 0L) return AppError.UrlImportEmptyResponse
        if (contentLength != null && contentLength > MAX_DOWNLOAD_BYTES) return AppError.UrlImportTooLarge
        return null
    }

    fun sanitizeUrl(input: String): String {
        return parseHttpUri(input)?.let(::sanitizeUrl).orEmpty()
    }

    fun sanitizeDisplayName(name: String?): String {
        val cleaned = name
            ?.replace(Regex("[\\r\\n\\t]"), " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "URL media"
        return cleaned.take(MAX_DISPLAY_NAME_CHARS)
    }

    fun isXiaoyuzhouEpisode(input: String): Boolean {
        return classify(input) == UrlImportSourceKind.XIAOYUZHOU_EPISODE
    }

    private fun parseHttpUri(input: String): URI? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null
        return runCatching { URI(trimmed) }
            .getOrNull()
            ?.normalize()
            ?.takeIf { it.scheme.equals("http", ignoreCase = true) || it.scheme.equals("https", ignoreCase = true) }
            ?.takeIf { !it.host.isNullOrBlank() }
    }

    private fun classify(uri: URI): UrlImportSourceKind {
        val host = uri.host.lowercase()
        val path = uri.rawPath.orEmpty()
        if ((host == "xiaoyuzhoufm.com" || host.endsWith(".xiaoyuzhoufm.com")) &&
            Regex("^/episode/[A-Za-z0-9_-]+/?$").matches(path)
        ) {
            return UrlImportSourceKind.XIAOYUZHOU_EPISODE
        }
        val extension = path.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
        if (extension in MEDIA_EXTENSIONS) return UrlImportSourceKind.DIRECT_MEDIA
        if (extension in RSS_EXTENSIONS) return UrlImportSourceKind.RSS_ENCLOSURE
        return UrlImportSourceKind.UNSUPPORTED
    }

    private fun displayNameFor(uri: URI, kind: UrlImportSourceKind): String {
        val pathName = uri.path
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
        val fallback = when (kind) {
            UrlImportSourceKind.XIAOYUZHOU_EPISODE -> "Xiaoyuzhou episode"
            UrlImportSourceKind.DIRECT_MEDIA -> "URL media"
            UrlImportSourceKind.RSS_ENCLOSURE -> "RSS enclosure"
            UrlImportSourceKind.UNSUPPORTED -> "URL media"
        }
        return sanitizeDisplayName(pathName ?: fallback)
    }

    private fun sanitizeUrl(uri: URI): String {
        val port = if (uri.port >= 0) ":${uri.port}" else ""
        val path = uri.rawPath?.takeIf { it.isNotBlank() } ?: "/"
        return "${uri.scheme.lowercase()}://${uri.host.lowercase()}$port$path"
    }

    private val MEDIA_EXTENSIONS = setOf(
        "mp3",
        "m4a",
        "aac",
        "wav",
        "flac",
        "ogg",
        "opus",
        "mp4",
        "mkv",
        "mov",
        "webm",
        "3gp"
    )

    private val RSS_EXTENSIONS = setOf("rss", "xml")
}
