package com.gptx.app.repo
import com.gptx.app.model.Message
import com.gptx.app.network.ChatApi
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

class ChatRepository {
    private val api = ChatApi()
    private val json = Json { ignoreUnknownKeys = true }
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    fun sendMessage(userMessage: String): Flow<String> {
        val userMsg = Message("user", userMessage)
        val assistantMsg = Message("assistant", "")
        _messages.value = (_messages.value + userMsg + assistantMsg).takeLast(16)
        return api.streamChat(_messages.value).onEach { chunk ->
            runCatching {
                val content = json.parseToJsonElement(chunk).jsonObject["choices"]
                    ?.jsonArray?.firstOrNull()?.jsonObject?.get("delta")?.jsonObject
                    ?.get("content")?.jsonPrimitive?.content
                if (content != null) {
                    _messages.value = _messages.value.map {
                        if (it == assistantMsg) it.copy(content = it.content + content) else it
                    }
                }
            }
        }
    }
}
