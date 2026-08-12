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
import kotlinx.coroutines.Job
import javax.inject.Inject

data class MediaGalleryUiState(
    val chatId: Long = 0,
    val activeTab: String = "images", // "images", "events"
    val photoMessages: List<ChatMessage> = emptyList(),
    val eventMessages: List<ChatMessage> = emptyList(),
    val selectedImageIndex: Int? = null,
    val selectedImagePath: String? = null,
    val downloadedPaths: Map<Long, String> = emptyMap(),
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
    private val downloadJobs = mutableMapOf<Long, Job>()

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
            if (_uiState.value.isLoading) return@launch
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val photosRes = chatRepository.getPhotoMessages(chatId, 0)
            val eventsRes = chatRepository.getEventMessages(chatId, 0)

            _uiState.update {
                it.copy(
                    photoMessages = photosRes.getOrDefault(emptyList()),
                    eventMessages = eventsRes.getOrDefault(emptyList()),
                    isLoading = false,
                    errorMessage = photosRes.exceptionOrNull()?.message ?: eventsRes.exceptionOrNull()?.message
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
        _uiState.update { it.copy(selectedImageIndex = index) }
        ensureImage(index)
    }

    fun ensureImage(index: Int) {
        val message = _uiState.value.photoMessages.getOrNull(index) ?: return
        _uiState.value.downloadedPaths[message.id]?.takeIf { java.io.File(it).exists() }?.let { path ->
            _uiState.update { it.copy(selectedImagePath = if (it.selectedImageIndex == index) path else it.selectedImagePath) }; return
        }
        val local = message.localMediaPath?.takeIf { java.io.File(it).exists() }
        if (local != null) {
            _uiState.update { it.copy(downloadedPaths = it.downloadedPaths + (message.id to local), selectedImagePath = if (it.selectedImageIndex == index) local else it.selectedImagePath) }
            return
        }
        val suffix = message.fileUrl?.substringAfterLast('.', "jpg")?.substringBefore('?')?.take(5) ?: "jpg"
        val destination = java.io.File(context.cacheDir, "chat_media_${message.id}.$suffix")
        if (destination.exists() && destination.length() > 0) {
            _uiState.update { it.copy(downloadedPaths = it.downloadedPaths + (message.id to destination.absolutePath), selectedImagePath = if (it.selectedImageIndex == index) destination.absolutePath else it.selectedImagePath) }; return
        }
        if (downloadJobs[message.id]?.isActive == true) return
        downloadJobs[message.id] = viewModelScope.launch {
            _uiState.update { it.copy(downloadingMessageId = message.id, errorMessage = null) }
            chatRepository.downloadMedia(chatId, message.id, destination).onSuccess { file ->
                _uiState.update { it.copy(downloadedPaths = it.downloadedPaths + (message.id to file.absolutePath), selectedImagePath = if (it.selectedImageIndex == index) file.absolutePath else it.selectedImagePath, downloadingMessageId = null) }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Unable to download media", downloadingMessageId = null) }
            }
            downloadJobs.remove(message.id)
        }
    }

    fun retrySelected() { _uiState.value.selectedImageIndex?.let(::ensureImage) }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }
}
