package com.gptx.app.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(modifier: Modifier, vm: ChatViewModel) {
    val state by vm.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Column(modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.messages) { msg ->
                Card(colors = CardDefaults.cardColors(
                    containerColor = if (msg.role == "user") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                ), modifier = Modifier.fillMaxWidth(if (msg.role == "user") 0.8f else 1f)) {
                    Text(msg.content, Modifier.padding(12.dp))
                }
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp)) }
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), label = { Text("Message") }, enabled = !state.isStreaming)
            Spacer(Modifier.width(8.dp))
            Button({ if (input.isNotBlank()) { vm.sendMessage(input); input = "" } }, enabled = !state.isStreaming && input.isNotBlank()) {
                Icon(Icons.Default.Send, "Send")
            }
        }
    }
}
