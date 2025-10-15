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
                    ?.jsonObject?.get("delta")?.jsonObject?.get("content")?.jsonPrimitive?.content.orEmpty()
                if (content.isNotBlank()) {
                    _messages.value = _messages.value.map {
                        if (it == assistantMsg) it.copy(content = it.content + content) else it
                    }
                }
            }
# Continue from Theme.kt
cat > ui/theme/Theme.kt << 'EOF'
package com.gptx.app.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF82B1FF),
    secondary = Color(0xFF64FFDA),
    tertiary = Color(0xFFFFAB40),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    error = Color(0xFFCF6679)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    secondary = Color(0xFF00BFA5),
    tertiary = Color(0xFFFF6F00),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    error = Color(0xFFB00020)
)

@Composable
fun GPTXTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
