package com.autopaymax.domain.auth.usecase

import android.app.Activity
import com.autopaymax.domain.auth.model.AuthUser
import com.autopaymax.domain.auth.repository.AuthRepository
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(activity: Activity): Result<AuthUser> =
        repository.signInWithGoogle(activity)
}
