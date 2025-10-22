package com.example.appui.di

import android.content.Context
import com.example.appui.data.local.VoiceConversation.VoiceConversationDao
import com.example.appui.data.local.VoiceConversation.VoiceConversationDatabase
import com.example.appui.domain.repository.ConversationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVoiceConversationDatabase(
        @ApplicationContext context: Context
    ): VoiceConversationDatabase {
        return VoiceConversationDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideVoiceConversationDao(
        database: VoiceConversationDatabase
    ): VoiceConversationDao {
        return database.conversationDao()
    }

    @Provides
    @Singleton
    fun provideConversationRepository(
        @ApplicationContext context: Context,
        dao: VoiceConversationDao
    ): ConversationRepository {
        return ConversationRepository(context, dao)
    }
}
