// domain/usecase/CheckAppUpdateUseCase.kt
package com.example.appui.domain.usecase

import com.example.appui.domain.model.UpdateInfo
import com.example.appui.domain.repository.AppUpdateRepository
import com.example.appui.utils.Either
import javax.inject.Inject

class CheckAppUpdateUseCase @Inject constructor(
    private val repository: AppUpdateRepository
) {
    suspend operator fun invoke(): Either<String, UpdateInfo> {
        return repository.checkForUpdate()
    }
}
