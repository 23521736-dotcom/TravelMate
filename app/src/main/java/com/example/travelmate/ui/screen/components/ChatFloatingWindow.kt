package com.example.travelmate.ui.screen.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.travelmate.data.model.GroqMessage
import com.example.travelmate.ui.theme.BluePrimary
import com.example.travelmate.ui.theme.GrayBackground
import com.example.travelmate.ui.theme.GrayText
import com.example.travelmate.ui.viewmodel.ChatViewModel
import com.example.travelmate.util.VoiceToTextParser
import kotlin.math.roundToInt

@Composable
fun ChatFloatingWindow(
    isVisible: Boolean,
    onClose: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val voiceParser = remember { VoiceToTextParser(context.applicationContext as android.app.Application) }
    val voiceState by voiceParser.state.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var canRecord by remember { mutableStateOf(false) }

    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            canRecord = isGranted
            if (isGranted) {
                voiceParser.startListening()
            }
        }
    )

    // Append voice text to input field
    LaunchedEffect(voiceState.spokenText) {
        if (voiceState.spokenText.isNotEmpty()) {
            inputText = voiceState.spokenText
        }
    }

    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp, end = 16.dp, start = 16.dp, top = 80.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Card(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                    .shadow(12.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Modern Header with Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(BluePrimary, Color(0xFF1E88E5))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(36.dp),
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.padding(8.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "TravelMate AI",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = if (uiState.isLoading) "Đang suy nghĩ..." else "Trực tuyến",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Row {
                                IconButton(onClick = { /* Could clear chat */ }) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.7f))
                                }
                                IconButton(onClick = onClose) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                }
                            }
                        }
                    }

                    // Messages List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF8F9FA))
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(uiState.messages.filter { it.role != "system" }) { message ->
                            ChatMessageItem(message)
                        }
                        
                        if (uiState.isLoading) {
                            item {
                                TypingIndicator()
                            }
                        }
                        
                        if (uiState.error != null) {
                            item {
                                ErrorMessage(uiState.error!!, onRetry = { viewModel.clearError() })
                            }
                        }
                    }

                    // Quick Prompts
                    if (!uiState.isLoading) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8F9FA))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.quickPrompts) { prompt ->
                                Surface(
                                    onClick = { viewModel.sendMessage(prompt) },
                                    color = Color.White,
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = prompt,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 12.sp,
                                        color = BluePrimary
                                    )
                                }
                            }
                        }
                    }

                    // Premium Input Area
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Column {
                            // Listening Progress/Real-time text
                            AnimatedVisibility(
                                visible = voiceState.isListening || voiceState.error != null,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (voiceState.error != null) Color.Red.copy(alpha = 0.1f) else BluePrimary.copy(alpha = 0.05f))
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (voiceState.isListening) {
                                        Icon(
                                            Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = BluePrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (voiceState.spokenText.isEmpty()) "Đang nghe..." else voiceState.spokenText,
                                            color = BluePrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    } else if (voiceState.error != null) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "Lỗi micro", color = Color.Red, fontSize = 12.sp)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Hỏi AI về chuyến đi của bạn...") },
                                    shape = RoundedCornerShape(24.dp),
                                    maxLines = 4,
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                if (voiceState.isListening) {
                                                    voiceParser.stopListening()
                                                } else {
                                                    if (canRecord) {
                                                        voiceParser.startListening()
                                                    } else {
                                                        recordAudioLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (voiceState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                                                contentDescription = "Voice Input",
                                                tint = if (voiceState.isListening) Color.Red else GrayText
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BluePrimary,
                                        unfocusedBorderColor = Color(0xFFE0E0E0),
                                        focusedContainerColor = Color(0xFFF1F3F4),
                                        unfocusedContainerColor = Color(0xFFF1F3F4)
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = {
                                        if (inputText.isNotBlank() && !uiState.isLoading) {
                                            viewModel.sendMessage(inputText)
                                            inputText = ""
                                            keyboardController?.hide()
                                        }
                                    })
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                FloatingActionButton(
                                    onClick = {
                                        if (inputText.isNotBlank() && !uiState.isLoading) {
                                            viewModel.sendMessage(inputText)
                                            inputText = ""
                                            keyboardController?.hide()
                                        }
                                    },
                                    containerColor = if (inputText.isNotBlank()) BluePrimary else Color.LightGray,
                                    contentColor = Color.White,
                                    modifier = Modifier.size(48.dp),
                                    shape = CircleShape,
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: GroqMessage) {
    val isUser = message.role == "user"
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (isUser) BluePrimary else Color.White,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(min = 40.dp, max = 280.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = if (isUser) Color.White else Color.Black,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 1.dp
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(text = "AI đang soạn thảo...", fontSize = 12.sp, color = GrayText)
            }
        }
    }
}

@Composable
fun ErrorMessage(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = error, color = Color.Red, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        TextButton(onClick = onRetry) {
            Text("Xóa thông báo lỗi", fontSize = 11.sp)
        }
    }
}
