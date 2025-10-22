package com.example.appui.data.local.VoiceConversation

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.appui.BuildConfig
import com.example.appui.data.model.Converters
import com.example.appui.data.model.VoiceConversation

@Database(
    entities = [VoiceConversation::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VoiceConversationDatabase : RoomDatabase() {
    abstract fun conversationDao(): VoiceConversationDao

    companion object {
        @Volatile
        private var INSTANCE: VoiceConversationDatabase? = null

        fun getDatabase(context: Context): VoiceConversationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoiceConversationDatabase::class.java,
                    "voice_conversation_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
