package com.gptx.app.ui
import androidx.lifecycle.*
import com.gptx.app.model.Message
import com.gptx.app.repo.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(private val repo: ChatRepository) : ViewModel() {
    data class UiState(
        val messages: List<Message> = emptyList(),
        val isStreaming: Boolean = false,
        val error: String? = null
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.messages.collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
    }

    fun sendMessage(input: String) {
        if (input.isBlank() || _uiState.value.isStreaming) return
        _uiState.update { it.copy(isStreaming = true, error = null) }
        viewModelScope.launch {
            runCatching { repo.sendMessage(input).collect {} }
                .onFailure { err -> _uiState.update { it.copy(error = "Error: ${err.message}") } }
            _uiState.update { it.copy(isStreaming = false) }
        }
    }

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(ChatRepository()) as T
        }
    }
}
