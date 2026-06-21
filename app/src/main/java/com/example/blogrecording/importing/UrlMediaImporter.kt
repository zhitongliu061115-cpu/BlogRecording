package com.example.blogrecording.importing

import android.content.Context
import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

data class ResolvedUrlMedia(
    val mediaUrl: String,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long?,
    val sourceKind: UrlImportSourceKind,
    val pageUrl: String,
    val sourceHost: String
)

data class DownloadedUrlMedia(
    val file: File,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long?,
    val source: UrlImportValidation
)

class UrlMediaImporter(
    private val cacheDir: File,
    private val client: OkHttpClient = defaultClient()
) {
    constructor(
        context: Context,
        client: OkHttpClient = defaultClient()
    ) : this(context.cacheDir, client)

    suspend fun resolve(source: UrlImportValidation): AppResult<ResolvedUrlMedia> {
        return when (source.kind) {
            UrlImportSourceKind.XIAOYUZHOU_EPISODE -> resolveXiaoyuzhou(source)
            UrlImportSourceKind.DIRECT_MEDIA -> AppResult.Success(
                ResolvedUrlMedia(
                    mediaUrl = source.originalUrl,
                    displayName = source.displayName,
                    mimeType = null,
                    sizeBytes = null,
                    sourceKind = source.kind,
                    pageUrl = source.sanitizedUrl,
                    sourceHost = source.host
                )
            )
            UrlImportSourceKind.RSS_ENCLOSURE -> resolveRss(source)
            UrlImportSourceKind.UNSUPPORTED -> AppResult.Failure(AppError.UrlImportUnsupported)
        }
    }

    suspend fun download(
        source: UrlImportValidation,
        resolved: ResolvedUrlMedia
    ): AppResult<DownloadedUrlMedia> {
        val request = Request.Builder()
            .url(resolved.mediaUrl)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        return execute(request) { response ->
            UrlMediaImportPolicy.classifyResponse(response.code, response.body?.contentLength())
                ?.let { return@execute AppResult.Failure(it) }
            val body = response.body ?: return@execute AppResult.Failure(AppError.UrlImportEmptyResponse)
            val contentLength = body.contentLength().takeIf { it >= 0L }
            val mimeType = response.header("Content-Type")?.substringBefore(';')?.trim()
                ?: resolved.mimeType
            val displayName = resolved.displayName.takeIf { it.isNotBlank() }
                ?: source.displayName
            val safe = LocalMediaImportPolicy.validate(displayName, mimeType, contentLength)
            if (safe is AppResult.Failure) {
                return@execute AppResult.Failure(
                    if (safe.error == AppError.LocalMediaTooLarge) AppError.UrlImportTooLarge else AppError.UrlImportUnsupported
                )
            }
            val dir = File(cacheDir, "url_media_imports").also { it.mkdirs() }
            val file = File.createTempFile("url-import-", ".media", dir)
            var copied = 0L
            try {
                body.byteStream().use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            copied += read
                            if (copied > UrlMediaImportPolicy.MAX_DOWNLOAD_BYTES) {
                                return@execute AppResult.Failure(AppError.UrlImportTooLarge)
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                }
            } catch (_: IOException) {
                file.delete()
                return@execute AppResult.Failure(AppError.UrlImportDownloadFailed)
            }
            if (copied <= 0L) {
                file.delete()
                return@execute AppResult.Failure(AppError.UrlImportEmptyResponse)
            }
            AppResult.Success(
                DownloadedUrlMedia(
                    file = file,
                    displayName = LocalMediaImportPolicy.sanitizeDisplayName(displayName),
                    mimeType = mimeType,
                    sizeBytes = copied,
                    source = source
                )
            )
        }
    }

    private suspend fun resolveXiaoyuzhou(source: UrlImportValidation): AppResult<ResolvedUrlMedia> {
        val request = Request.Builder()
            .url(source.originalUrl)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        return execute(request) { response ->
            UrlMediaImportPolicy.classifyResponse(response.code, response.body?.contentLength())
                ?.let { return@execute AppResult.Failure(it) }
            val html = response.body?.string().orEmpty()
            if (html.isBlank()) return@execute AppResult.Failure(AppError.UrlImportEmptyResponse)
            XiaoyuzhouMediaParser.parse(html, source)
        }
    }

    private suspend fun resolveRss(source: UrlImportValidation): AppResult<ResolvedUrlMedia> {
        val request = Request.Builder()
            .url(source.originalUrl)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        return execute(request) { response ->
            UrlMediaImportPolicy.classifyResponse(response.code, response.body?.contentLength())
                ?.let { return@execute AppResult.Failure(it) }
            val xml = response.body?.string().orEmpty()
            if (xml.isBlank()) return@execute AppResult.Failure(AppError.UrlImportEmptyResponse)
            RssEnclosureParser.parse(xml, source)
        }
    }

    private suspend fun <T> execute(
        request: Request,
        block: (Response) -> AppResult<T>
    ): AppResult<T> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                client.newCall(request).execute().use(block)
            } catch (_: SocketTimeoutException) {
                AppResult.Failure(AppError.UrlImportTimeout)
            } catch (_: IllegalArgumentException) {
                AppResult.Failure(AppError.UrlImportInvalidUrl)
            } catch (_: IOException) {
                AppResult.Failure(AppError.UrlImportDownloadFailed)
            } catch (_: SecurityException) {
                AppResult.Failure(AppError.UrlImportDownloadFailed)
            }
        }
    }

    private companion object {
        const val USER_AGENT = "BlogRecording/1.0"

        fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }
    }
}

object XiaoyuzhouMediaParser {
    fun parse(html: String, source: UrlImportValidation): AppResult<ResolvedUrlMedia> {
        extractNextDataJson(html)?.let { json ->
            findMediaUrl(json)?.let { mediaUrl ->
                return AppResult.Success(
                    ResolvedUrlMedia(
                        mediaUrl = mediaUrl,
                        displayName = UrlMediaImportPolicy.sanitizeDisplayName(findTitle(json) ?: source.displayName),
                        mimeType = null,
                        sizeBytes = null,
                        sourceKind = source.kind,
                        pageUrl = source.sanitizedUrl,
                        sourceHost = source.host
                    )
                )
            }
        }
        findJsonLd(html)?.let { jsonLd ->
            findMediaUrl(jsonLd)?.let { mediaUrl ->
                return AppResult.Success(
                    ResolvedUrlMedia(
                        mediaUrl = mediaUrl,
                        displayName = UrlMediaImportPolicy.sanitizeDisplayName(findTitle(jsonLd) ?: source.displayName),
                        mimeType = null,
                        sizeBytes = null,
                        sourceKind = source.kind,
                        pageUrl = source.sanitizedUrl,
                        sourceHost = source.host
                    )
                )
            }
        }
        return if (html.contains("login", ignoreCase = true) || html.contains("登录")) {
            AppResult.Failure(AppError.UrlImportPrivateOrBlocked)
        } else {
            AppResult.Failure(AppError.UrlImportUnsupported)
        }
    }

    private fun extractNextDataJson(html: String): JSONObject? {
        val regex = Regex(
            "<script[^>]+id=[\"']__NEXT_DATA__[\"'][^>]*>(.*?)</script>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val raw = regex.find(html)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (raw.isBlank()) return null
        return runCatching { JSONObject(raw) }.getOrNull()
    }

    private fun findJsonLd(html: String): JSONObject? {
        val regex = Regex(
            "<script[^>]+type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        return regex.findAll(html).firstNotNullOfOrNull { match ->
            runCatching { JSONObject(match.groupValues[1].trim()) }.getOrNull()
        }
    }

    private fun findMediaUrl(value: Any?): String? {
        return walk(value)
            .filterIsInstance<String>()
            .firstOrNull { candidate ->
                candidate.startsWith("http", ignoreCase = true) &&
                    UrlMediaImportPolicy.classify(candidate) == UrlImportSourceKind.DIRECT_MEDIA
            }
    }

    private fun findTitle(value: Any?): String? {
        return walkNamed(value, setOf("title", "name", "episodeTitle"))
            .filterIsInstance<String>()
            .firstOrNull { it.isNotBlank() }
    }

    private fun walk(value: Any?): Sequence<Any?> = sequence {
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    yieldAll(walk(value.opt(keys.next())))
                }
            }
            is JSONArray -> {
                for (index in 0 until value.length()) yieldAll(walk(value.opt(index)))
            }
            else -> yield(value)
        }
    }

    private fun walkNamed(value: Any?, names: Set<String>): Sequence<Any?> = sequence {
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val child = value.opt(key)
                    if (key in names) yield(child)
                    yieldAll(walkNamed(child, names))
                }
            }
            is JSONArray -> {
                for (index in 0 until value.length()) yieldAll(walkNamed(value.opt(index), names))
            }
        }
    }
}

object RssEnclosureParser {
    fun parse(xml: String, source: UrlImportValidation): AppResult<ResolvedUrlMedia> {
        val enclosureRegex = Regex(
            "<enclosure\\b[^>]*url=[\"']([^\"']+)[\"'][^>]*>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val match = enclosureRegex.find(xml) ?: return AppResult.Failure(AppError.UrlImportUnsupported)
        val tag = match.value
        val mediaUrl = htmlDecode(match.groupValues[1])
        if (UrlMediaImportPolicy.classify(mediaUrl) != UrlImportSourceKind.DIRECT_MEDIA) {
            return AppResult.Failure(AppError.UrlImportUnsupported)
        }
        val type = Regex("type=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            .find(tag)
            ?.groupValues
            ?.getOrNull(1)
        val length = Regex("length=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            .find(tag)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
        return AppResult.Success(
            ResolvedUrlMedia(
                mediaUrl = mediaUrl,
                displayName = UrlMediaImportPolicy.sanitizeDisplayName(mediaUrl.substringAfterLast('/')),
                mimeType = type,
                sizeBytes = length,
                sourceKind = source.kind,
                pageUrl = source.sanitizedUrl,
                sourceHost = source.host
            )
        )
    }

    private fun htmlDecode(value: String): String {
        return value
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }
}
