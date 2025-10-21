package com.example.appui.data.repository

import com.example.appui.domain.model.Sample
import com.example.appui.domain.repository.SampleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SampleRepositoryImpl @Inject constructor() : SampleRepository {

    private val state = MutableStateFlow<List<Sample>>(emptyList())

    override fun observeSamples(): Flow<List<Sample>> = state.asStateFlow()

    override suspend fun addSample(item: Sample) {
        state.value = state.value + item
    }
}
