package com.autopaymax.domain.profile.usecase

import com.autopaymax.common.Resource
import com.autopaymax.domain.profile.repository.ProfileRepository
import javax.inject.Inject

class UpdateUserProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(
        name: String? = null,
        upiId: String? = null
    ): Resource<Unit> = repository.updateUserProfile(name, upiId)
}
