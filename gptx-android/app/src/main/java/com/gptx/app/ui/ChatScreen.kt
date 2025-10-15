package com.gptx.app.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, onToggleTheme: () -> Unit) {
    val uiState by viewModel.uiState
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var userInput by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(uiState.messages.size - 1) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPTX") },
                actions = {
                    IconButton(onClick = onToggleTheme) { Icon(Icons.Default.DarkMode, null) }
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Clear History") }, onClick = { viewModel.clearHistory(); showMenu = false })
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(Modifier.weight(1f), state = listState) {
                items(uiState.messages) { message ->
                    Text(message.content, Modifier.padding(8.dp))
                }
            }
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(value = userInput, onValueChange = { userInput = it }, Modifier.weight(1f))
                Button(onClick = { viewModel.sendMessage(userInput); userInput = "" }) { Icon(Icons.Default.Send, null) }
            }
        }
    }
}
