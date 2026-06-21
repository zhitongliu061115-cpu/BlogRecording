package com.example.blogrecording.importing

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UrlMediaImporterTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun parsesXiaoyuzhouNextDataMediaUrl() {
        val source = xiaoyuzhouSource()
        val html = """
            <html>
              <script id="__NEXT_DATA__" type="application/json">
                {"props":{"pageProps":{"episode":{"title":"边走边聊","media":{"url":"https://media.example.com/audio/ep.mp3?token=secret"}}}}}
              </script>
            </html>
        """.trimIndent()

        val result = XiaoyuzhouMediaParser.parse(html, source)

        require(result is AppResult.Success)
        assertEquals("https://media.example.com/audio/ep.mp3?token=secret", result.value.mediaUrl)
        assertEquals("边走边聊", result.value.displayName)
        assertEquals("https://www.xiaoyuzhoufm.com/episode/6a3392764233e62bc54be185", result.value.pageUrl)
        assertFalse(result.value.pageUrl.contains("token"))
    }

    @Test
    fun xiaoyuzhouLoginPageFailsAsPrivateOrBlocked() {
        val result = XiaoyuzhouMediaParser.parse("<html>请登录后查看</html>", xiaoyuzhouSource())

        assertEquals(AppResult.Failure(AppError.UrlImportPrivateOrBlocked), result)
    }

    @Test
    fun parsesRssEnclosureMedia() {
        val source = rssSource()
        val xml = """
            <rss><channel><item>
              <title>Episode</title>
              <enclosure url="https://cdn.example.com/e.mp3?download=1" type="audio/mpeg" length="42" />
            </item></channel></rss>
        """.trimIndent()

        val result = RssEnclosureParser.parse(xml, source)

        require(result is AppResult.Success)
        assertEquals("https://cdn.example.com/e.mp3?download=1", result.value.mediaUrl)
        assertEquals("audio/mpeg", result.value.mimeType)
        assertEquals(42L, result.value.sizeBytes)
    }

    @Test
    fun resolvesXiaoyuzhouBeforeFallbacks() = runBlocking {
        val importer = UrlMediaImporter(
            cacheDir = temp.newFolder(),
            client = fakeClient(
                200,
                """<script id="__NEXT_DATA__">{"episode":{"title":"E","audioUrl":"https://cdn.example.com/e.mp3"}}</script>"""
            )
        )

        val result = importer.resolve(xiaoyuzhouSource())

        require(result is AppResult.Success)
        assertEquals(UrlImportSourceKind.XIAOYUZHOU_EPISODE, result.value.sourceKind)
        assertEquals("https://cdn.example.com/e.mp3", result.value.mediaUrl)
    }

    @Test
    fun mapsNetworkResponsesWithoutRawBodies() = runBlocking {
        val source = xiaoyuzhouSource()
        val unauthorized = UrlMediaImporter(temp.newFolder(), fakeClient(401, "secret"))
            .resolve(source)
        val rateLimited = UrlMediaImporter(temp.newFolder(), fakeClient(429, "secret"))
            .resolve(source)
        val failed = UrlMediaImporter(temp.newFolder(), fakeClient(500, "secret"))
            .resolve(source)

        assertEquals(AppResult.Failure(AppError.UrlImportUnauthorized), unauthorized)
        assertEquals(AppResult.Failure(AppError.UrlImportRateLimited), rateLimited)
        assertEquals(AppResult.Failure(AppError.UrlImportHttpFailed(500)), failed)
    }

    @Test
    fun downloadsMediaToPrivateCache() = runBlocking {
        val source = directSource()
        val importer = UrlMediaImporter(
            cacheDir = temp.newFolder(),
            client = fakeClient(200, "audio bytes", "audio/mpeg")
        )
        val resolved = ResolvedUrlMedia(
            mediaUrl = source.originalUrl,
            displayName = "episode.mp3",
            mimeType = "audio/mpeg",
            sizeBytes = null,
            sourceKind = UrlImportSourceKind.DIRECT_MEDIA,
            pageUrl = source.sanitizedUrl,
            sourceHost = source.host
        )

        val result = importer.download(source, resolved)

        require(result is AppResult.Success)
        assertTrue(result.value.file.exists())
        assertTrue(result.value.file.absolutePath.contains("url_media_imports"))
        assertEquals("audio/mpeg", result.value.mimeType)
        assertEquals(11L, result.value.sizeBytes)
        assertTrue(result.value.file.delete())
    }

    @Test
    fun downloadRejectsUnsupportedContentType() = runBlocking {
        val source = directSource()
        val importer = UrlMediaImporter(
            cacheDir = temp.newFolder(),
            client = fakeClient(200, "plain text", "text/plain")
        )
        val resolved = ResolvedUrlMedia(
            mediaUrl = source.originalUrl,
            displayName = "episode.txt",
            mimeType = "text/plain",
            sizeBytes = null,
            sourceKind = UrlImportSourceKind.DIRECT_MEDIA,
            pageUrl = source.sanitizedUrl,
            sourceHost = source.host
        )

        val result = importer.download(source, resolved)

        assertEquals(AppResult.Failure(AppError.UrlImportUnsupported), result)
    }

    private fun xiaoyuzhouSource(): UrlImportValidation {
        return (UrlMediaImportPolicy.validate(
            "https://www.xiaoyuzhoufm.com/episode/6a3392764233e62bc54be185?token=secret"
        ) as AppResult.Success).value
    }

    private fun directSource(): UrlImportValidation {
        return (UrlMediaImportPolicy.validate("https://cdn.example.com/episode.mp3?token=secret") as AppResult.Success).value
    }

    private fun rssSource(): UrlImportValidation {
        return (UrlMediaImportPolicy.validate("https://example.com/feed.xml") as AppResult.Success).value
    }

    private fun fakeClient(
        code: Int,
        body: String,
        contentType: String = "text/html"
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("Test")
                    .body(body.toResponseBody(contentType.toMediaType()))
                    .build()
            }
            .build()
    }
}
