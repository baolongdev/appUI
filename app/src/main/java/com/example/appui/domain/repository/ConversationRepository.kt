package com.example.appui.domain.repository

import android.content.Context
import android.util.Log
import com.example.appui.data.local.VoiceConversation.VoiceConversationDao
import com.example.appui.data.model.ConversationMessage
import com.example.appui.data.model.Speaker
import com.example.appui.data.model.VoiceConversation
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val conversationDao: VoiceConversationDao
) {
    private val conversationDir = File(context.filesDir, "conversations")

    init {
        if (!conversationDir.exists()) {
            conversationDir.mkdirs()
        }
    }

    fun getAllConversations(): Flow<List<VoiceConversation>> {
        return conversationDao.getAllConversations()
    }

    suspend fun getConversation(id: Long): VoiceConversation? {
        return conversationDao.getConversationById(id)
    }

    suspend fun saveConversation(
        agentId: String,
        agentName: String?,
        messages: List<ConversationMessage>,
        durationMs: Long,
        mode: String,
        audioFile: File? = null,
        title: String? = null
    ): Long {
        val timestamp = System.currentTimeMillis()
        val generatedTitle = title ?: generateTitle(messages, timestamp)
        val autoTags = generateTags(messages, agentName, mode) // ✅ NEW

        val audioPath = audioFile?.let { file ->
            val destFile = File(conversationDir, "audio_${timestamp}.wav")
            file.copyTo(destFile, overwrite = true)
            destFile.absolutePath
        }

        val conversation = VoiceConversation(
            agentId = agentId,
            agentName = agentName, // ✅ NEW
            timestamp = timestamp,
            durationMs = durationMs,
            audioFilePath = audioPath,
            messages = messages,
            mode = mode,
            title = generatedTitle,
            tags = autoTags, // ✅ NEW
            isFavorite = false
        )

        return conversationDao.insertConversation(conversation)
    }

    /**
     * Auto-generate tags based on conversation content
     */
    private fun generateTags(
        messages: List<ConversationMessage>,
        agentName: String?,
        mode: String
    ): List<String> {
        val tags = mutableListOf<String>()

        // 1. Add agent name as tag
        agentName?.let { tags.add(it) }

        // 2. Add mode tag
        when (mode) {
            "FULL_DUPLEX" -> tags.add("Full Duplex")
            "HALF_DUPLEX" -> tags.add("Half Duplex")
            "PUSH_TO_TALK" -> tags.add("Push to Talk")
        }

        // 3. Analyze content and add topic tags
        val allText = messages.joinToString(" ") { it.text.lowercase() }

        // Common topics detection
        val topicKeywords = mapOf(
            "Tư vấn" to listOf("tư vấn", "hỗ trợ", "giúp đỡ", "hướng dẫn"),
            "Thanh toán" to listOf("thanh toán", "payment", "tiền", "giá"),
            "Đơn hàng" to listOf("đơn hàng", "order", "mua", "đặt hàng"),
            "Khiếu nại" to listOf("khiếu nại", "complaint", "vấn đề", "lỗi"),
            "Thông tin" to listOf("thông tin", "info", "chi tiết", "hỏi"),
            "Chào hỏi" to listOf("xin chào", "chào", "hello", "hi")
        )

        topicKeywords.forEach { (tag, keywords) ->
            if (keywords.any { allText.contains(it) } && !tags.contains(tag)) {
                tags.add(tag)
            }
        }

        // 4. Add duration-based tag
        val durationMinutes = messages.size / 2 // Rough estimate
        when {
            durationMinutes < 2 -> tags.add("Ngắn")
            durationMinutes < 5 -> tags.add("Trung bình")
            else -> tags.add("Dài")
        }

        // 5. Add date-based tag
        val dateFormat = SimpleDateFormat("MMM yyyy", Locale("vi", "VN"))
        tags.add(dateFormat.format(Date()))

        return tags.take(5) // Limit to 5 tags
    }

    suspend fun updateConversationTitle(id: Long, newTitle: String) {
        conversationDao.getConversationById(id)?.let { conversation ->
            conversationDao.updateConversation(
                conversation.copy(title = newTitle)
            )
        }
    }

    suspend fun updateTags(id: Long, tags: List<String>) {
        conversationDao.getConversationById(id)?.let { conversation ->
            conversationDao.updateConversation(
                conversation.copy(tags = tags)
            )
        }
    }

    suspend fun toggleFavorite(id: Long) {
        conversationDao.getConversationById(id)?.let { conversation ->
            conversationDao.updateConversation(
                conversation.copy(isFavorite = !conversation.isFavorite)
            )
        }
    }

    suspend fun deleteConversation(id: Long) {
        val conversation = conversationDao.getConversationById(id)

        conversation?.audioFilePath?.let { path ->
            File(path).delete()
        }

        conversation?.messages?.forEach { message ->
            message.audioSegmentPath?.let { path ->
                File(path).delete()
            }
        }

        conversationDao.deleteConversationById(id)
        Log.i(TAG, "Deleted conversation: $id")
    }

    fun exportToJson(conversation: VoiceConversation): String {
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .create()
        return gson.toJson(conversation)
    }

    fun exportToHtml(conversation: VoiceConversation): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(conversation.timestamp))

        val messagesHtml = conversation.messages.joinToString("\n") { message ->
            val speaker = if (message.speaker == Speaker.USER) "Bạn" else "Agent"
            val messageTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(Date(message.timestamp))

            """
            <div class="message ${if (message.speaker == Speaker.USER) "user" else "agent"}">
                <div class="speaker">$speaker</div>
                <div class="time">$messageTime</div>
                <div class="text">${message.text}</div>
            </div>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>${conversation.title ?: "Conversation"}</title>
            <style>
                body {
                    font-family: Arial, sans-serif;
                    max-width: 800px;
                    margin: 0 auto;
                    padding: 20px;
                    background: #f5f5f5;
                }
                .header {
                    background: white;
                    padding: 20px;
                    border-radius: 8px;
                    margin-bottom: 20px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                }
                .title {
                    font-size: 24px;
                    font-weight: bold;
                    margin-bottom: 10px;
                }
                .meta {
                    color: #666;
                    font-size: 14px;
                }
                .tags {
                    margin-top: 10px;
                }
                .tag {
                    display: inline-block;
                    background: #e3f2fd;
                    color: #1976d2;
                    padding: 4px 12px;
                    border-radius: 12px;
                    font-size: 12px;
                    margin-right: 8px;
                }
                .message {
                    background: white;
                    padding: 15px;
                    border-radius: 8px;
                    margin-bottom: 10px;
                    box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                }
                .message.user {
                    margin-left: 60px;
                    background: #e3f2fd;
                }
                .message.agent {
                    margin-right: 60px;
                    background: #fff3e0;
                }
                .speaker {
                    font-weight: bold;
                    font-size: 14px;
                    color: #333;
                }
                .time {
                    font-size: 12px;
                    color: #999;
                    margin-top: 4px;
                }
                .text {
                    margin-top: 8px;
                    line-height: 1.5;
                }
            </style>
        </head>
        <body>
            <div class="header">
                <div class="title">${conversation.title ?: "Untitled Conversation"}</div>
                <div class="meta">
                    <div>📅 $formattedDate</div>
                    <div>💬 ${conversation.messages.size} tin nhắn</div>
                    <div>⏱️ ${formatDuration(conversation.durationMs)}</div>
                    <div>🤖 Agent: ${conversation.agentId}</div>
                </div>
                ${if (conversation.tags.isNotEmpty()) """
                <div class="tags">
                    ${conversation.tags.joinToString("") { "<span class='tag'>$it</span>" }}
                </div>
                """ else ""}
            </div>
            
            <div class="messages">
                $messagesHtml
            </div>
            
            <div style="text-align: center; color: #999; font-size: 12px; margin-top: 40px;">
                Exported from Voice Assistant App
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun generateTitle(messages: List<ConversationMessage>, timestamp: Long): String {
        val firstUserMessage = messages.firstOrNull { it.speaker == Speaker.USER }
        return if (firstUserMessage != null) {
            val preview = firstUserMessage.text.take(30)
            if (firstUserMessage.text.length > 30) "$preview..." else preview
        } else {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            "Conversation ${dateFormat.format(Date(timestamp))}"
        }
    }

    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    companion object {
        private const val TAG = "ConversationRepository"
    }
}
