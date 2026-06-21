package com.example.blogrecording.summary

import com.example.blogrecording.common.AppResult
import com.example.blogrecording.data.AppSettings
import com.example.blogrecording.data.StructuredSummaryParseStatus
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
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
                val content = if (callCount < 3) {
                    "partial-$callCount"
                } else {
                    """{"overview":"final-summary","keyPoints":["p"],"actionItems":[],"openQuestions":[],"quoteCandidates":[]}"""
                }
                val responseBody = JSONObject()
                    .put(
                        "choices",
                        JSONArray().put(
                            JSONObject().put(
                                "message",
                                JSONObject().put("content", content)
                            )
                        )
                    )
                    .toString()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(responseBody.toResponseBody(JSON))
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

        require(result is AppResult.Success)
        assertEquals("final-summary\n\n关键要点\n- p", result.value.text)
        assertEquals("final-summary", result.value.structured.overview)
        assertEquals(StructuredSummaryParseStatus.PARTIAL, result.value.structured.parseStatus)
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
