package com.uj.appstorysautopaymanager.domain.auth.usecase

import android.app.Activity
import com.uj.appstorysautopaymanager.domain.auth.model.AuthUser
import com.uj.appstorysautopaymanager.domain.auth.repository.AuthRepository
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(activity: Activity): Result<AuthUser> =
        repository.signInWithGoogle(activity)
}
