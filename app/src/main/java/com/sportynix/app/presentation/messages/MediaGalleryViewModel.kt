package com.sportynix.app.presentation.messages

import androidx.lifecycle.SavedStateHandle
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportynix.app.domain.model.ChatMessage
import com.sportynix.app.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaGalleryUiState(
    val chatId: Long = 0,
    val activeTab: String = "images", // "images", "events"
    val photoMessages: List<ChatMessage> = emptyList(),
    val eventMessages: List<ChatMessage> = emptyList(),
    val selectedImageIndex: Int? = null,
    val selectedImagePath: String? = null,
    val downloadingMessageId: Long? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class MediaGalleryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatId: Long = savedStateHandle.get<String>("chatId")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(MediaGalleryUiState(chatId = chatId))
    val uiState: StateFlow<MediaGalleryUiState> = _uiState.asStateFlow()

    init {
        if (chatId > 0) {
            loadMedia()
        }
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val photosRes = chatRepository.getPhotoMessages(chatId, 0)
            val eventsRes = chatRepository.getEventMessages(chatId, 0)

            _uiState.update {
                it.copy(
                    photoMessages = photosRes.getOrDefault(emptyList()),
                    eventMessages = eventsRes.getOrDefault(emptyList()),
                    isLoading = false
                )
            }
        }
    }

    fun setActiveTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setSelectedImageIndex(index: Int?) {
        if (index == null) {
            _uiState.update { it.copy(selectedImageIndex = null, selectedImagePath = null, downloadingMessageId = null) }
            return
        }
        val message = _uiState.value.photoMessages.getOrNull(index) ?: return
        val local = message.localMediaPath?.takeIf { java.io.File(it).exists() }
        if (local != null) {
            _uiState.update { it.copy(selectedImageIndex = index, selectedImagePath = local) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(downloadingMessageId = message.id, errorMessage = null) }
            val suffix = message.fileUrl?.substringAfterLast('.', "jpg")?.substringBefore('?')?.take(5) ?: "jpg"
            val destination = java.io.File(context.cacheDir, "chat_media_${message.id}.$suffix")
            chatRepository.downloadMedia(chatId, message.id, destination).onSuccess { file ->
                _uiState.update { it.copy(selectedImageIndex = index, selectedImagePath = file.absolutePath, downloadingMessageId = null) }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Unable to download media", downloadingMessageId = null) }
            }
        }
    }
}
