package com.gptx.app.repo
import com.gptx.app.model.Message
import com.gptx.app.network.ChatApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class ChatRepository {
    private val api = ChatApi()
    private val json = Json { ignoreUnknownKeys = true }

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: Flow<List<Message>> = _messages.asStateFlow()

    fun sendMessage(userMessage: String): Flow<String> {
        val newUserMessage = Message(role = "user", content = userMessage)
        val newAssistantMessage = Message(role = "assistant", content = "")

        _messages.update { currentMessages ->
            (currentMessages + newUserMessage + newAssistantMessage).takeLast(16)
        }

        return api.streamChat(_messages.value).onEach { chunk ->
            try {
                val jsonElement = json.parseToJsonElement(chunk)
                val choicesArray = jsonElement.jsonObject["choices"]?.jsonArray
                if (choicesArray != null && choicesArray.size > 0) {
                    val firstChoice = choicesArray[0].jsonObject
                    val delta = firstChoice["delta"]?.jsonObject
                    if (delta != null) {
                        val contentElement = delta["content"]
                        if (contentElement != null) {
                            val content = contentElement.jsonPrimitive.content
                            if (content.isNotBlank()) {
                                _messages.update { currentList ->
                                    currentList.map { msg ->
                                        if (msg == newAssistantMessage) {
                                            msg.copy(content = msg.content + content)
                                        } else {
                                            msg
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently ignore parsing errors
            }
        }
    }

    fun clearHistory() {
        _messages.value = emptyList()
    }
}
