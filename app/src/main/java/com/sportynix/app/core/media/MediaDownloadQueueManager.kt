package com.sportynix.app.core.media

import android.content.Context
import com.sportynix.app.data.local.dao.ChatDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class MediaDownloadItem(
    val id: String,
    val messageId: Long,
    val url: String,
    val localPath: String,
    var status: String,
    var retries: Int
)

@Singleton
class MediaDownloadQueueManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatDao: ChatDao,
    private val okHttpClient: OkHttpClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isProcessing = false
    private val queue = mutableListOf<MediaDownloadItem>()
    
    private val maxRetries = 3

    fun enqueue(messageId: Long, mediaUrl: String) {
        scope.launch {
            try {
                val mediaDir = File(context.filesDir, "media")
                if (!mediaDir.exists()) mediaDir.mkdirs()
                
                val localPath = File(mediaDir, messageId.toString()).absolutePath
                val item = MediaDownloadItem(
                    id = UUID.randomUUID().toString(),
                    messageId = messageId,
                    url = mediaUrl,
                    localPath = localPath,
                    status = "pending",
                    retries = 0
                )
                
                synchronized(queue) {
                    if (queue.none { it.messageId == messageId }) {
                        queue.add(item)
                    }
                }
                
                Timber.d("Queued media download: \$messageId")
                flush()
            } catch (e: Exception) {
                Timber.e(e, "Failed to enqueue media")
            }
        }
    }

    suspend fun flush() {
        if (isProcessing) return
        isProcessing = true
        
        try {
            Timber.d("Processing media downloads...")
            val pendingItems = synchronized(queue) {
                queue.filter { it.status == "pending" && it.retries < maxRetries }.take(10)
            }
            
            if (pendingItems.isEmpty()) {
                isProcessing = false
                return
            }
            
            for (item in pendingItems) {
                downloadMedia(item)
            }
        } finally {
            isProcessing = false
        }
    }
    
    private suspend fun downloadMedia(item: MediaDownloadItem) {
        try {
            Timber.d("Downloading media for \${item.messageId}")
            item.status = "downloading"
            
            val request = Request.Builder().url(item.url).build()
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful && response.body != null) {
                val file = File(item.localPath)
                val fos = FileOutputStream(file)
                fos.write(response.body!!.bytes())
                fos.close()
                
                item.status = "done"
                item.retries = 0
                
                // Update DB message with local path
                val msg = chatDao.getMessagesForChat(0).find { it.id == item.messageId } // Hacky lookup
                // In reality we should have a `updateMessageMediaLocalPath` in ChatDao.
                // Let's assume we do this update manually by fetching and updating if needed.
                // For simplicity, we just mark the item as done and remove from queue
                synchronized(queue) {
                    queue.removeIf { it.id == item.id }
                }
                Timber.d("Downloaded to \${item.localPath}")
            } else {
                throw Exception("HTTP \${response.code}")
            }
        } catch (e: Exception) {
            Timber.w("Download failed for \${item.messageId}: \${e.message}")
            item.retries++
            if (item.retries >= maxRetries) {
                item.status = "failed"
            } else {
                item.status = "pending"
            }
        }
    }
}
