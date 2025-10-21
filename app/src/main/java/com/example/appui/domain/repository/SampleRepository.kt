package com.example.appui.domain.repository

import com.example.appui.domain.model.Sample
import kotlinx.coroutines.flow.Flow

interface SampleRepository {
    fun observeSamples(): Flow<List<Sample>>
    suspend fun addSample(item: Sample)
}
