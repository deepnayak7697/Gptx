package com.gptx.app.network
import com.gptx.app.BuildConfig
import com.gptx.app.GPTXApp
import com.gptx.app.model.Message
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class ChatApi {
    private val client = GPTXApp.instance.okHttpClient
    private val json = Json { ignoreUnknownKeys = true }

    fun streamChat(messages: List<Message>): Flow<String> = flow {
        val body = json.encodeToString(mapOf("messages" to messages, "stream" to true))
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${BuildConfig.API_BASE_URL}/v1/chat")
            .addHeader("X-App-Key", BuildConfig.APP_KEY)
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            response.body?.source()?.let { source ->
                while (!source.exhausted()) {
                    source.readUtf8Line()?.let { line ->
                        if (line.startsWith("data: ") && !line.contains("[DONE]")) {
                            emit(line.substringAfter("data: "))
                        }
                    }
                }
            }
        }
    }
}
