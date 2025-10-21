package com.example.appui.domain.usecase

import com.example.appui.domain.model.Sample
import com.example.appui.domain.repository.SampleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSampleUseCase @Inject constructor(
    private val repo: SampleRepository
) {
    operator fun invoke(): Flow<List<Sample>> = repo.observeSamples()
}
