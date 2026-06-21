package com.example.blogrecording.qa

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import com.example.blogrecording.common.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class DeepSeekQaClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val endpoint: String = "https://api.deepseek.com/chat/completions"
) {
    suspend fun answer(apiKey: String, model: String, prompt: String): AppResult<String> {
        if (apiKey.isBlank()) return AppResult.Failure(AppError.DeepSeekApiKeyMissing)
        val body = JSONObject()
            .put("model", model)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                    .put(JSONObject().put("role", "user").put("content", prompt))
            )
            .put("temperature", 0.1)
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    when (response.code) {
                        401 -> AppResult.Failure(AppError.DeepSeekUnauthorized)
                        429 -> AppResult.Failure(AppError.DeepSeekRateLimited)
                        in 200..299 -> parseQaResponse(response.body?.string().orEmpty())
                        else -> {
                            Logger.warn("DeepSeek QA failed with HTTP ${response.code}")
                            AppResult.Failure(AppError.NetworkFailed("HTTP ${response.code}"))
                        }
                    }
                }
            } catch (error: IOException) {
                Logger.warn("DeepSeek QA network failed: ${error.javaClass.simpleName}")
                AppResult.Failure(AppError.NetworkFailed(error.javaClass.simpleName))
            } catch (error: SecurityException) {
                Logger.warn("DeepSeek QA permission failed: ${error.javaClass.simpleName}")
                AppResult.Failure(AppError.NetworkFailed("缺少网络权限"))
            }
        }
    }

    private fun parseQaResponse(raw: String): AppResult<String> {
        if (raw.isBlank()) {
            Logger.warn("DeepSeek QA response was empty")
            return AppResult.Failure(AppError.NetworkFailed("DeepSeek 返回为空"))
        }
        val json = try {
            JSONObject(raw)
        } catch (error: JSONException) {
            Logger.warn("DeepSeek QA JSON parse failed: ${error.javaClass.simpleName}; length=${raw.length}")
            return AppResult.Failure(AppError.Unknown("DeepSeek 返回不是有效 JSON"))
        }
        json.optJSONObject("error")?.let { error ->
            val message = error.optString("message")
                .takeIf { it.isNotBlank() && it != "null" }
                ?: error.optString("code", "DeepSeek error")
            Logger.warn("DeepSeek QA API error; fields=${error.safeKeys()}")
            return AppResult.Failure(AppError.NetworkFailed(message.take(MAX_ERROR_MESSAGE_CHARS)))
        }
        val choices = json.optJSONArray("choices")
        val firstChoice = choices?.optJSONObject(0)
        val message = firstChoice?.optJSONObject("message")
        val content = message?.optString("content")
            ?.takeIf { it.isNotBlank() && it != "null" }
            ?: firstChoice?.optString("text")?.takeIf { it.isNotBlank() && it != "null" }
        if (content.isNullOrBlank()) {
            Logger.warn(
                "DeepSeek QA content missing; topLevel=${json.safeKeys()}; " +
                    "choice=${firstChoice?.safeKeys().orEmpty()}; message=${message?.safeKeys().orEmpty()}; " +
                    "length=${raw.length}"
            )
            return AppResult.Failure(AppError.Unknown("DeepSeek 响应缺少问答内容"))
        }
        return AppResult.Success(content.trim())
    }

    private fun JSONObject.safeKeys(): String {
        val keys = keys()
        val names = mutableListOf<String>()
        while (keys.hasNext()) {
            names += keys.next()
        }
        return names.joinToString(separator = ",")
    }

    private companion object {
        const val SYSTEM_PROMPT = "Answer only from the provided episode context."
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val MAX_ERROR_MESSAGE_CHARS = 160
    }
}
