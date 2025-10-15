package com.gptx.app.repo
import com.gptx.app.model.Message
import com.gptx.app.network.ChatApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChatRepository {
    private val api = ChatApi()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    fun sendMessage(userMessage: String): Flow<String> {
        val newUserMessage = Message(role = "user", content = userMessage)
        val newAssistantMessage = Message(role = "assistant", content = "")

        _messages.update { currentMessages ->
            (currentMessages + newUserMessage + newAssistantMessage).takeLast(16)
        }

        return api.streamChat(_messages.value)
    }

    fun clearHistory() {
        _messages.value = emptyList()
    }
}
