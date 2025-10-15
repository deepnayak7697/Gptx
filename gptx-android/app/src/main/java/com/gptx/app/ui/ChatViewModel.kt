package com.gptx.app.ui
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gptx.app.model.Message
import com.gptx.app.model.StreamResponse
import com.gptx.app.repo.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ChatViewModel : ViewModel() {
    private val repository = ChatRepository()
    private val json = Json { ignoreUnknownKeys = true }

    data class ChatUiState(
        val messages: List<Message> = emptyList(),
        val isStreaming: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.messages.collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun sendMessage(userInput: String) {
        if (userInput.isBlank() || _uiState.value.isStreaming) return

        _uiState.update { it.copy(isStreaming = true, error = null) }

        viewModelScope.launch {
            try {
                repository.sendMessage(userInput).collect { chunk ->
                    try {
                        // Parse the JSON chunk using our data class
                        val streamResponse = json.decodeFromString<StreamResponse>(chunk)
                        val content = streamResponse.choices.firstOrNull()?.delta?.content ?: ""
                        
                        if (content.isNotBlank()) {
                            _uiState.update { currentState ->
                                val currentMessages = currentState.messages.toMutableList()
                                if (currentMessages.isNotEmpty()) {
                                    val lastMessage = currentMessages.last()
                                    if (lastMessage.role == "assistant") {
                                        currentMessages[currentMessages.size - 1] = 
                                            lastMessage.copy(content = lastMessage.content + content)
                                    }
                                }
                                currentState.copy(messages = currentMessages)
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore parsing errors for empty chunks or malformed data
                        if (chunk.isNotBlank()) {
                            println("Error parsing chunk '$chunk': ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to send message: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isStreaming = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearHistory() {
        repository.clearHistory()
    }
}
