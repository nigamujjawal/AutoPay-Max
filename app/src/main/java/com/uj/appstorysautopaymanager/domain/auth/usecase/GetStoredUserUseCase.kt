package com.uj.appstorysautopaymanager.domain.auth.usecase

import com.uj.appstorysautopaymanager.domain.auth.model.AuthUser
import com.uj.appstorysautopaymanager.domain.auth.repository.AuthRepository
import javax.inject.Inject

class GetStoredUserUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): AuthUser? = repository.getStoredUser()
}
