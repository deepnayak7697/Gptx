package com.gptx.app.repo
import com.gptx.app.model.Message
import com.gptx.app.network.ChatApi
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.io.InputStream

class ChatRepository {
    private val api = ChatApi()
    private val json = Json { ignoreUnknownKeys = true }
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    fun sendMessage(userMessage: String, imageUrl: String? = null): Flow<String> {
        val userMsg = Message("user", userMessage, imageUrl)
        val assistantMsg = Message("assistant", "")
        _messages.value = (_messages.value + userMsg + assistantMsg).takeLast(20)
        return api.streamChat(_messages.value).onEach { chunk ->
            try {
                val jsonElement = json.parseToJsonElement(chunk)
                val content = jsonElement.jsonObject["choices"]?.jsonArray?.getOrNull(0)
                    ?.jsonObject?.get("delta")?.jsonObject?.get("content")
                    ?.jsonPrimitive?.content.orEmpty()
                if (content.isNotBlank()) {
                    _messages.value = _messages.value.map {
                        if (it == assistantMsg) it.copy(content = it.content + content) else it
                    }
                }
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }
    }

    suspend fun uploadImage(inputStream: InputStream, mimeType: String): String {
        return api.uploadFile(inputStream, mimeType)
    }

    fun clearHistory() {
        _messages.value = emptyList()
    }
}
