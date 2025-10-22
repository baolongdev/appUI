package com.example.appui.data.local.VoiceConversation

import androidx.room.*
import com.example.appui.data.model.VoiceConversation
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceConversationDao {
    @Query("SELECT * FROM voice_conversations ORDER BY timestamp DESC")
    fun getAllConversations(): Flow<List<VoiceConversation>>

    @Query("SELECT * FROM voice_conversations WHERE id = :id")
    suspend fun getConversationById(id: Long): VoiceConversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: VoiceConversation): Long

    @Update
    suspend fun updateConversation(conversation: VoiceConversation)

    @Delete
    suspend fun deleteConversation(conversation: VoiceConversation)

    @Query("DELETE FROM voice_conversations WHERE id = :id")
    suspend fun deleteConversationById(id: Long)

    @Query("SELECT COUNT(*) FROM voice_conversations")
    suspend fun getConversationCount(): Int
}
