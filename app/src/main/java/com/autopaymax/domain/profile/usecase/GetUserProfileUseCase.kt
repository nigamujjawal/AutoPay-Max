package com.autopaymax.domain.profile.usecase

import com.autopaymax.common.Resource
import com.autopaymax.domain.profile.model.UserProfile
import com.autopaymax.domain.profile.repository.ProfileRepository
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(): Resource<UserProfile> = repository.getUserProfile()
}
