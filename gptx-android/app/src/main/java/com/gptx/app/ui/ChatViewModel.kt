package com.gptx.app.ui
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gptx.app.model.Message
import com.gptx.app.repo.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val repository = ChatRepository()

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
                    _uiState.update { currentState ->
                        val currentMessages = currentState.messages.toMutableList()
                        if (currentMessages.isNotEmpty()) {
                            val lastMessage = currentMessages.last()
                            if (lastMessage.role == "assistant") {
                                currentMessages[currentMessages.size - 1] = 
                                    lastMessage.copy(content = lastMessage.content + chunk)
                            }
                        }
                        currentState.copy(messages = currentMessages)
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
