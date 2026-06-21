package com.example.blogrecording.qa

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

class DeepSeekQaClientTest {
    @Test
    fun blankApiKeyDoesNotSendRequest() = runBlocking {
        val result = DeepSeekQaClient(client = errorClient())
            .answer(apiKey = " ", model = "deepseek-chat", prompt = "prompt")

        assertEquals(AppResult.Failure(AppError.DeepSeekApiKeyMissing), result)
    }

    @Test
    fun mapsProviderErrorsSafely() = runBlocking {
        val unauthorized = DeepSeekQaClient(fakeClient(401, "{}"))
            .answer(apiKey = "key", model = "model", prompt = "prompt")
        val rateLimited = DeepSeekQaClient(fakeClient(429, "{}"))
            .answer(apiKey = "key", model = "model", prompt = "prompt")
        val server = DeepSeekQaClient(fakeClient(500, "raw body"))
            .answer(apiKey = "key", model = "model", prompt = "prompt")

        assertEquals(AppResult.Failure(AppError.DeepSeekUnauthorized), unauthorized)
        assertEquals(AppResult.Failure(AppError.DeepSeekRateLimited), rateLimited)
        assertEquals(AppResult.Failure(AppError.NetworkFailed("HTTP 500")), server)
    }

    @Test
    fun parsesAnswerAndDoesNotPutApiKeyInBody() = runBlocking {
        lateinit var capturedRequest: Request
        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                capturedRequest = chain.request()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"choices":[{"message":{"content":"answer"}}]}""".toResponseBody(JSON))
                    .build()
            }
            .build()

        val result = DeepSeekQaClient(httpClient)
            .answer(apiKey = "secret-key", model = "deepseek-chat", prompt = "episode context")

        assertEquals(AppResult.Success("answer"), result)
        val body = requestBody(capturedRequest)
        assertTrue(body.contains("episode context"))
        assertFalse(body.contains("secret-key"))
        assertEquals("Bearer secret-key", capturedRequest.header("Authorization"))
    }

    @Test
    fun emptyAndMalformedResponsesFailSafely() = runBlocking {
        val empty = DeepSeekQaClient(fakeClient(200, ""))
            .answer(apiKey = "key", model = "model", prompt = "prompt")
        val malformed = DeepSeekQaClient(fakeClient(200, "not-json"))
            .answer(apiKey = "key", model = "model", prompt = "prompt")

        assertTrue(empty is AppResult.Failure)
        assertTrue(malformed is AppResult.Failure)
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
