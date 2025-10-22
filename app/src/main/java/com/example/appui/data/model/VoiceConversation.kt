package com.example.appui.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "voice_conversations")
data class VoiceConversation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val agentId: String,
    val agentName: String?,
    val timestamp: Long,
    val durationMs: Long,
    val audioFilePath: String?,
    val messages: List<ConversationMessage>,
    val mode: String,
    val title: String? = null,
    val tags: List<String> = emptyList(), // ✅ NEW
    val isFavorite: Boolean = false // ✅ NEW
)

data class ConversationMessage(
    val timestamp: Long,
    val speaker: Speaker,
    val text: String,
    val audioSegmentPath: String? = null,
    val vadScore: Float = 0f
)

enum class Speaker {
    USER,
    AGENT
}

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromMessageList(value: List<ConversationMessage>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toMessageList(value: String): List<ConversationMessage> {
        val listType = object : TypeToken<List<ConversationMessage>>() {}.type
        return gson.fromJson(value, listType)
    }

    // ✅ NEW: Tag converters
    @TypeConverter
    fun fromTagList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toTagList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
}
