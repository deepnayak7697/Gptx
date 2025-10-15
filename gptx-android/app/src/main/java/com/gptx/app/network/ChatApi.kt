package com.gptx.app.network
import com.gptx.app.BuildConfig
import com.gptx.app.GPTXApp
import com.gptx.app.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class ChatApi {
    private val client = GPTXApp.instance.okHttpClient
    private val json = Json { ignoreUnknownKeys = true }

    fun streamChat(messages: List<Message>): Flow<String> = flow {
        val requestBody = mapOf(
            "model" to "gpt-4o-mini",
            "messages" to messages,
            "stream" to true
        )
        val body = json.encodeToString(requestBody).toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(BuildConfig.API_BASE_URL + "/v1/chat")
            .addHeader("X-App-Key", BuildConfig.APP_KEY)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP error: ${response.code} ${response.message}")
            val source = response.body?.source() ?: throw Exception("Response body is null")
            while (!source.exhausted()) {
                val line = source.readUtf8Line()
                if (line != null) {
                    if (line.startsWith("data: ") && !line.contains("[DONE]")) {
                        val data = line.substringAfter("data: ")
                        emit(data)
                    } else if (line.contains("[DONE]")) {
                        break
                    }
                }
            }
        }
    }
}
