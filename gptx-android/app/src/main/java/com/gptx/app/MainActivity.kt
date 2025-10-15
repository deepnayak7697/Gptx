package com.gptx.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gptx.app.ui.ChatScreen
import com.gptx.app.ui.ChatViewModel
import com.gptx.app.ui.theme.GPTXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppContent()
        }
    }

    @Composable
    private fun AppContent() {
        var darkMode by remember { mutableStateOf(isSystemInDarkTheme()) }
        GPTXTheme(darkTheme = darkMode) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                val viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory())
                ChatScreen(viewModel = viewModel, onToggleTheme = { darkMode = !darkMode })
            }
        }
    }
}
