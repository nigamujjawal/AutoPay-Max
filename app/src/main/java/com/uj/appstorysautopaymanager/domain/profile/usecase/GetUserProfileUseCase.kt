package com.uj.appstorysautopaymanager.domain.profile.usecase

import com.uj.appstorysautopaymanager.common.Resource
import com.uj.appstorysautopaymanager.domain.profile.model.UserProfile
import com.uj.appstorysautopaymanager.domain.profile.repository.ProfileRepository
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(): Resource<UserProfile> = repository.getUserProfile()
}
