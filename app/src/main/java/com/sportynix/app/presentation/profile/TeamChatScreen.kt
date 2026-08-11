package com.sportynix.app.presentation.profile

import androidx.compose.runtime.Composable
import com.sportynix.app.presentation.messages.ChatScreen

@Composable
fun TeamChatScreen(
    conversationId: String,
    onNavigateBack: () -> Unit
) {
    val chatId = conversationId.toLongOrNull() ?: 0L
    ChatScreen(
        chatId = chatId,
        onNavigateBack = onNavigateBack,
        onNavigateToInfo = { },
        onNavigateToGallery = { }
    )
}
