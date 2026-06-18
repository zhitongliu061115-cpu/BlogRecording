package com.example.blogrecording.summary

import com.example.blogrecording.common.AppResult
import com.example.blogrecording.data.AppSettings
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
import org.junit.Test

class SummaryRepositoryTest {
    @Test
    fun longTranscriptUsesChunkSummariesThenFinalMerge() = runBlocking {
        val requestBodies = mutableListOf<String>()
        var callCount = 0
        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                callCount += 1
                requestBodies += requestBody(chain.request())
                val content = if (callCount < 3) "partial-$callCount" else "final-summary"
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"choices":[{"message":{"content":"$content"}}]}""".toResponseBody(JSON))
                    .build()
            }
            .build()
        val repository = SummaryRepository(
            client = DeepSeekSummaryClient(httpClient),
            chunker = TranscriptChunker(maxCharsPerChunk = 18)
        )
        val transcript = "a".repeat(8) + "\n\n" + "b".repeat(8) + "\n\n" + "c".repeat(8)

        val result = repository.generateSummary(
            apiKey = "secret-key",
            transcript = transcript,
            settings = AppSettings(deepSeekModel = "deepseek-chat")
        )

        assertEquals(AppResult.Success("final-summary"), result)
        assertEquals(3, callCount)
        assertTrue(requestBodies[0].contains("a".repeat(8)))
        assertTrue(requestBodies[1].contains("c".repeat(8)))
        assertTrue(requestBodies[2].contains("partial-1"))
        assertTrue(requestBodies[2].contains("partial-2"))
        assertFalse(requestBodies.any { it.contains("secret-key") })
    }

    private fun requestBody(request: Request): String {
        val buffer = okio.Buffer()
        request.body?.writeTo(buffer)
        return buffer.readUtf8()
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
