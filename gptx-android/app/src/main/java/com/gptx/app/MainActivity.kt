package com.gptx.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gptx.app.ui.ChatScreen
import com.gptx.app.ui.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory())
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    ChatScreen(Modifier.padding(padding), viewModel)
                }
            }
        }
    }
}
