package com.gptx.app.ui
import androidx.lifecycle.*
import com.gptx.app.model.Message
import com.gptx.app.repo.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream

class ChatViewModel(private val repo: ChatRepository) : ViewModel() {
    data class UiState(
        val messages: List<Message> = emptyList(),
        val isStreaming: Boolean = false,
        val error: String? = null,
        val uploadingImage: Boolean = false
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

    fun sendMessage(input: String, imageUrl: String? = null) {
        if (input.isBlank() || _uiState.value.isStreaming) return
        _uiState.update { it.copy(isStreaming = true, error = null) }
        viewModelScope.launch {
            runCatching { repo.sendMessage(input, imageUrl).collect {} }
                .onFailure { err -> _uiState.update { it.copy(error = "Error: ${err.message}") } }
            _uiState.update { it.copy(isStreaming = false) }
        }
    }

    fun uploadImage(inputStream: InputStream, mimeType: String, onSuccess: (String) -> Unit) {
        _uiState.update { it.copy(uploadingImage = true, error = null) }
        viewModelScope.launch {
            runCatching { repo.uploadImage(inputStream, mimeType) }
                .onSuccess { url -> 
                    onSuccess(url)
                    _uiState.update { it.copy(uploadingImage = false) }
                }
                .onFailure { err -> 
                    _uiState.update { it.copy(uploadingImage = false, error = "Upload failed: ${err.message}") }
                }
        }
    }

    fun clearHistory() {
        repo.clearHistory()
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(ChatRepository()) as T
        }
    }
}
