// domain/usecase/GetAllReleasesUseCase.kt
package com.example.appui.domain.usecase

import com.example.appui.domain.model.AppRelease
import com.example.appui.domain.repository.AppUpdateRepository
import com.example.appui.utils.Either
import javax.inject.Inject

class GetAllReleasesUseCase @Inject constructor(
    private val repository: AppUpdateRepository
) {
    suspend operator fun invoke(): Either<String, List<AppRelease>> {
        return repository.getAllReleases()
    }
}
