package com.autopaymax.domain.auth.usecase

import com.autopaymax.domain.auth.model.AuthUser
import com.autopaymax.domain.auth.repository.AuthRepository
import javax.inject.Inject

class GetStoredUserUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): AuthUser? = repository.getStoredUser()
}
