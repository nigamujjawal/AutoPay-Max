package com.uj.appstorysautopaymanager.domain.profile.usecase

import com.uj.appstorysautopaymanager.common.Resource
import com.uj.appstorysautopaymanager.domain.profile.repository.ProfileRepository
import javax.inject.Inject

class UpdateUserProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(
        name: String? = null,
        upiId: String? = null
    ): Resource<Unit> = repository.updateUserProfile(name, upiId)
}
