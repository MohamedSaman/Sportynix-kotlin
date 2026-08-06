package com.sportynix.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.sportynix.app.core.network.WebSocketManager
import com.sportynix.app.data.remote.api.ChatApiService
import com.sportynix.app.data.remote.dto.ChatMessageDto
import com.sportynix.app.data.remote.dto.SendMessageRequestDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sportynix.app.presentation.theme.DarkBackground
import com.sportynix.app.presentation.theme.LightBackground
import com.sportynix.app.presentation.theme.LocalThemeController
import com.sportynix.app.presentation.theme.NeonGreen
import com.sportynix.app.presentation.theme.SportynixGreenPrimary

data class TeamChatState(val messages: List<ChatMessageDto> = emptyList(), val draft: String = "", val loading: Boolean = false, val sending: Boolean = false, val error: String? = null)

@HiltViewModel
class TeamChatViewModel @Inject constructor(private val api: ChatApiService, private val webSocket: WebSocketManager, private val gson: Gson) : ViewModel() {
    private val _state = MutableStateFlow(TeamChatState())
    val state = _state.asStateFlow()
    private var chatId: String? = null
    fun load(id: String) {
        if (chatId == id && _state.value.messages.isNotEmpty()) return
        chatId = id; _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try { val result = api.getMessages(id); if (!result.isSuccessful) error("Failed to load chat"); _state.value = _state.value.copy(messages = result.body()?.results.orEmpty(), loading = false); webSocket.connect("ws/chat/$id/"); launch { webSocket.incomingMessages.collect { raw -> parseIncoming(raw)?.let { msg -> if (_state.value.messages.none { it.id == msg.id }) _state.value = _state.value.copy(messages = _state.value.messages + msg) } } } } catch (e: Exception) { _state.value = _state.value.copy(loading = false, error = e.message ?: "Unable to load chat") }
        }
    }
    fun setDraft(value: String) { _state.value = _state.value.copy(draft = value) }
    fun send() {
        val id = chatId ?: return; val text = _state.value.draft.trim(); if (text.isEmpty() || _state.value.sending) return
        _state.value = _state.value.copy(sending = true, draft = "")
        viewModelScope.launch { try { val result = api.sendMessage(id, SendMessageRequestDto(text)); if (!result.isSuccessful) error("Failed to send message"); result.body()?.let { _state.value = _state.value.copy(messages = _state.value.messages + it) }; webSocket.sendJson(JsonObject().apply { addProperty("message", text); addProperty("message_type", "text") }); _state.value = _state.value.copy(sending = false) } catch (e: Exception) { _state.value = _state.value.copy(sending = false, draft = text, error = e.message ?: "Unable to send message") } }
    }
    fun clearError() { _state.value = _state.value.copy(error = null) }
    override fun onCleared() { webSocket.disconnect(); super.onCleared() }
    private fun parseIncoming(raw: String): ChatMessageDto? = runCatching { val o = gson.fromJson(raw, JsonObject::class.java); if (o.get("type")?.asString !in setOf("message", "chat_message")) return null; ChatMessageDto(o.get("message_id")?.asString ?: o.get("id")?.asString ?: return null, o.get("message")?.asString ?: o.get("content")?.asString, null, o.get("timestamp")?.asString, o.get("message_type")?.asString ?: "text", false) }.getOrNull()
}

@Composable
fun TeamChatScreen(conversationId: String, onNavigateBack: () -> Unit, viewModel: TeamChatViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState(); val dark = LocalThemeController.current.isDark; val green = if (dark) NeonGreen else SportynixGreenPrimary
    LaunchedEffect(conversationId) { viewModel.load(conversationId) }
    Scaffold(containerColor = if (dark) DarkBackground else LightBackground, topBar = { TopAppBar(title = { Text("Team chat") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }, bottomBar = { Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = state.draft, onValueChange = viewModel::setDraft, modifier = Modifier.weight(1f), placeholder = { Text("Message") }, singleLine = true); IconButton(onClick = viewModel::send, enabled = !state.sending && state.draft.isNotBlank()) { Icon(Icons.Default.Send, "Send", tint = green) } } }) { padding ->
        if (state.loading) Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator(color = green) } else LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(state.messages, key = { it.id }) { message -> Surface(color = green.copy(.14f), shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Text(message.content.orEmpty(), modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurface) } } }
    }
    state.error?.let { AlertDialog(onDismissRequest = viewModel::clearError, title = { Text("Chat") }, text = { Text(it) }, confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK", color = green) } }) }
}
