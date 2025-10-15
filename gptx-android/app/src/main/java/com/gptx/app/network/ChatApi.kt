package com.gptx.app.network
import com.gptx.app.BuildConfig
import com.gptx.app.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ChatApi {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    
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
            if (!response.isSuccessful) {
                throw Exception("HTTP error: ${response.code} - ${response.message}")
            }
            
            response.body?.let { responseBody ->
                responseBody.byteStream().bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        // Handle Server-Sent Events format
                        when {
                            line.startsWith("data: ") && line != "data: [DONE]" -> {
                                val jsonData = line.substringAfter("data: ")
                                if (jsonData.isNotBlank() && jsonData != "[DONE]") {
                                    emit(jsonData)
                                }
                            }
                            line.trim().isNotBlank() && !line.startsWith(":") -> {
                                // Some SSE implementations might not use "data: " prefix
                                if (line != "[DONE]") {
                                    emit(line)
                                }
                            }
                        }
                    }
                }
            } ?: throw Exception("Response body is null")
        }
    }
}
