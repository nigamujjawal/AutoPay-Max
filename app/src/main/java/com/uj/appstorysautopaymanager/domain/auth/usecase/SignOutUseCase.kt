package com.uj.appstorysautopaymanager.domain.auth.usecase

import com.uj.appstorysautopaymanager.domain.auth.repository.AuthRepository
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() = repository.signOut()
}
