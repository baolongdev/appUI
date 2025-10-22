package com.example.appui.ui.screen.history

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appui.data.model.VoiceConversation
import com.example.appui.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ConversationHistoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ConversationRepository
) : ViewModel() {

    val conversations: StateFlow<List<VoiceConversation>> = repository
        .getAllConversations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            repository.deleteConversation(id)
        }
    }

    fun updateTags(id: Long, tags: List<String>) {
        viewModelScope.launch {
            repository.updateTags(id, tags)
        }
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    /**
     * Share as HTML with proper filename
     */
    fun shareAsHtml(conversation: VoiceConversation) {
        viewModelScope.launch {
            try {
                val htmlContent = repository.exportToHtml(conversation)
                val fileName = sanitizeFileName(conversation.title ?: "Conversation") + ".html"

                val file = withContext(Dispatchers.IO) {
                    File(context.cacheDir, fileName).apply {
                        writeText(htmlContent)
                    }
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/html"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, conversation.title ?: "Conversation")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val chooserIntent = Intent.createChooser(shareIntent, "Chia sẻ HTML").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(chooserIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Share as JSON with proper filename
     */
    fun shareAsJson(conversation: VoiceConversation) {
        viewModelScope.launch {
            try {
                val jsonContent = repository.exportToJson(conversation)
                val fileName = sanitizeFileName(conversation.title ?: "Conversation") + ".json"

                val file = withContext(Dispatchers.IO) {
                    File(context.cacheDir, fileName).apply {
                        writeText(jsonContent)
                    }
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, conversation.title ?: "Conversation")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val chooserIntent = Intent.createChooser(shareIntent, "Chia sẻ JSON").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(chooserIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Sanitize filename to remove invalid characters
     */
    private fun sanitizeFileName(name: String): String {
        return name
            .replace(Regex("[^a-zA-Z0-9._\\-àáảãạâầấẩẫậăằắẳẵặèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđÀÁẢÃẠÂẦẤẨẪẬĂẰẮẲẴẶÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴĐ ]"), "_")
            .replace(Regex("\\s+"), "_")
            .take(50) // Limit length
    }

    // Legacy compatibility
    fun quickShareHtml(conversation: VoiceConversation) = shareAsHtml(conversation)
    fun quickShareJson(conversation: VoiceConversation) = shareAsJson(conversation)
}
