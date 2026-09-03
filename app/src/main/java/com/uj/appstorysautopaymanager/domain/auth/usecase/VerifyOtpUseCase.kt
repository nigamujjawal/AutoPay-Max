package com.uj.appstorysautopaymanager.domain.auth.usecase

import com.uj.appstorysautopaymanager.domain.auth.model.AuthUser
import com.uj.appstorysautopaymanager.domain.auth.repository.AuthRepository
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(verificationId: String, code: String): Result<AuthUser> =
        repository.verifyOtp(verificationId, code)
}
