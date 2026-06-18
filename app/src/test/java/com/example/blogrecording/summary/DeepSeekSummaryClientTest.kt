package com.example.blogrecording.summary

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
import org.junit.Test

class DeepSeekSummaryClientTest {
    @Test
    fun blankApiKeyDoesNotSendRequest() = runBlocking {
        val client = DeepSeekSummaryClient(client = errorClient())

        val result = client.summarize(apiKey = " ", model = "deepseek-chat", prompt = "hello")

        assertEquals(AppResult.Failure(AppError.DeepSeekApiKeyMissing), result)
    }

    @Test
    fun unauthorizedAndRateLimitedResponsesMapToSpecificErrors() = runBlocking {
        val unauthorized = DeepSeekSummaryClient(fakeClient(401, "{}"))
            .summarize(apiKey = "key", model = "model", prompt = "prompt")
        val rateLimited = DeepSeekSummaryClient(fakeClient(429, "{}"))
            .summarize(apiKey = "key", model = "model", prompt = "prompt")

        assertEquals(AppResult.Failure(AppError.DeepSeekUnauthorized), unauthorized)
        assertEquals(AppResult.Failure(AppError.DeepSeekRateLimited), rateLimited)
    }

    @Test
    fun nonSuccessResponseDoesNotExposeBody() = runBlocking {
        val result = DeepSeekSummaryClient(fakeClient(500, "secret body"))
            .summarize(apiKey = "key", model = "model", prompt = "prompt")

        assertEquals(AppResult.Failure(AppError.NetworkFailed("HTTP 500")), result)
    }

    @Test
    fun emptyAndMalformedSuccessResponsesFailSafely() = runBlocking {
        val empty = DeepSeekSummaryClient(fakeClient(200, ""))
            .summarize(apiKey = "key", model = "model", prompt = "prompt")
        val malformed = DeepSeekSummaryClient(fakeClient(200, "not-json"))
            .summarize(apiKey = "key", model = "model", prompt = "prompt")

        assertTrue(empty is AppResult.Failure)
        assertTrue(malformed is AppResult.Failure)
    }

    @Test
    fun requestContainsPromptButNotRawSecretInBody() = runBlocking {
        lateinit var capturedRequest: Request
        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                capturedRequest = chain.request()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"choices":[{"message":{"content":"summary"}}]}""".toResponseBody(JSON))
                    .build()
            }
            .build()

        val result = DeepSeekSummaryClient(httpClient)
            .summarize(apiKey = "secret-key", model = "deepseek-chat", prompt = "speaker transcript")

        assertEquals(AppResult.Success("summary"), result)
        val body = requestBody(capturedRequest)
        assertTrue(body.contains("speaker transcript"))
        assertFalse(body.contains("secret-key"))
        assertEquals("Bearer secret-key", capturedRequest.header("Authorization"))
    }

    private fun fakeClient(code: Int, body: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("Test")
                    .body(body.toResponseBody(JSON))
                    .build()
            }
            .build()
    }

    private fun errorClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { error("Network should not be called") }
            .build()
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
