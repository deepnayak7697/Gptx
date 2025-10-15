package com.gptx.app.ui
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, onToggleTheme: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedImageUrl by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            context.contentResolver.openInputStream(it)?.use { stream ->
                val mimeType = context.contentResolver.getType(it) ?: "image/jpeg"
                viewModel.uploadImage(stream, mimeType) { url ->
                    uploadedImageUrl = url
                }
            }
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(state.messages.size - 1) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ChatBubble, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("GPTX v2.0", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Spacer(Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text("Advanced", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(Icons.Default.DarkMode, "Toggle theme")
                    }
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, "Menu")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Clear History") },
                                onClick = { viewModel.clearHistory(); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Delete, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Chat") },
                                onClick = {
                                    val text = state.messages.joinToString("\n\n") { 
                                        "${it.role.uppercase()}: ${it.content}" 
                                    }
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, text)
                                        putExtra(Intent.EXTRA_SUBJECT, "GPTX Chat Export")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Export chat"))
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Share, null) }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.messages) { msg ->
                    MessageBubble(msg)
                }
                if (state.isStreaming) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    repeat(3) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSecondaryContainer)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = state.error != null) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(state.error ?: "", Modifier.weight(1f), fontSize = 14.sp)
                        IconButton(onClick = { viewModel.dismissError() }) {
                            Icon(Icons.Default.Close, "Dismiss")
                        }
                    }
                }
            }

            AnimatedVisibility(visible = selectedImageUri != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        if (state.uploadingImage) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Uploading...")
                        } else {
                            Text("Image ready", Modifier.weight(1f))
                        }
                        IconButton(onClick = {
                            selectedImageUri = null
                            uploadedImageUrl = null
                        }) {
                            Icon(Icons.Default.Close, "Remove")
                        }
                    }
                }
            }

            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { imagePicker.launch("image/*") },
                        enabled = !state.isStreaming
                    ) {
                        Icon(Icons.Default.Image, "Attach image", tint = MaterialTheme.colorScheme.primary)
                    }
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask me anything...") },
                        enabled = !state.isStreaming,
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 5
                    )
                    FilledIconButton(
                        onClick = {
                            if (input.isNotBlank()) {
                                viewModel.sendMessage(input, uploadedImageUrl)
                                input = ""
                                selectedImageUri = null
                                uploadedImageUrl = null
                            }
                        },
                        enabled = !state.isStreaming && input.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, "Send")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: com.gptx.app.model.Message) {
    val isUser = message.role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer 
                   else MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.imageUrl != null) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    text = message.content,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer 
                           else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
