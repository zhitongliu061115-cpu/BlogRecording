package com.example.blogrecording.importing

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlMediaImportPolicyTest {
    @Test
    fun acceptsXiaoyuzhouEpisodeUrlAndSanitizesQuery() {
        val result = UrlMediaImportPolicy.validate(
            "https://www.xiaoyuzhoufm.com/episode/6a3392764233e62bc54be185?token=secret"
        )

        require(result is AppResult.Success)
        assertEquals(UrlImportSourceKind.XIAOYUZHOU_EPISODE, result.value.kind)
        assertEquals("www.xiaoyuzhoufm.com", result.value.host)
        assertEquals(
            "https://www.xiaoyuzhoufm.com/episode/6a3392764233e62bc54be185",
            result.value.sanitizedUrl
        )
        assertFalse(result.value.sanitizedUrl.contains("token"))
    }

    @Test
    fun acceptsDirectMediaByExtension() {
        val result = UrlMediaImportPolicy.validate("https://cdn.example.com/show/episode.m4a")

        require(result is AppResult.Success)
        assertEquals(UrlImportSourceKind.DIRECT_MEDIA, result.value.kind)
        assertEquals("episode.m4a", result.value.displayName)
    }

    @Test
    fun acceptsRssEnclosureCandidateByExtension() {
        val result = UrlMediaImportPolicy.validate("https://example.com/feed.xml")

        require(result is AppResult.Success)
        assertEquals(UrlImportSourceKind.RSS_ENCLOSURE, result.value.kind)
    }

    @Test
    fun rejectsInvalidOrUnsupportedUrls() {
        val invalid = UrlMediaImportPolicy.validate("ftp://example.com/episode.mp3")
        val unsupported = UrlMediaImportPolicy.validate("https://example.com/page")

        assertTrue(invalid is AppResult.Failure)
        assertEquals(AppError.UrlImportInvalidUrl, (invalid as AppResult.Failure).error)
        assertTrue(unsupported is AppResult.Failure)
        assertEquals(AppError.UrlImportUnsupported, (unsupported as AppResult.Failure).error)
    }

    @Test
    fun classifiesHttpResponsesWithoutRawBodies() {
        assertEquals(AppError.UrlImportUnauthorized, UrlMediaImportPolicy.classifyResponse(401, 10L))
        assertEquals(AppError.UrlImportUnauthorized, UrlMediaImportPolicy.classifyResponse(403, 10L))
        assertEquals(AppError.UrlImportRateLimited, UrlMediaImportPolicy.classifyResponse(429, 10L))
        assertEquals(AppError.UrlImportHttpFailed(500), UrlMediaImportPolicy.classifyResponse(500, 10L))
        assertEquals(AppError.UrlImportEmptyResponse, UrlMediaImportPolicy.classifyResponse(200, 0L))
        assertEquals(
            AppError.UrlImportTooLarge,
            UrlMediaImportPolicy.classifyResponse(200, UrlMediaImportPolicy.MAX_DOWNLOAD_BYTES + 1L)
        )
        assertEquals(null, UrlMediaImportPolicy.classifyResponse(200, 10L))
    }

    @Test
    fun displayNameSanitizerRemovesControlCharactersAndBoundsLength() {
        val name = UrlMediaImportPolicy.sanitizeDisplayName(" episode\nwith\tsecret.mp3 ".repeat(20))

        assertFalse(name.contains("\n"))
        assertFalse(name.contains("\t"))
        assertTrue(name.length <= 120)
    }
}
