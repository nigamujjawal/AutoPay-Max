package com.uj.appstorysautopaymanager.domain.auth.usecase

import android.app.Activity
import com.uj.appstorysautopaymanager.domain.auth.model.OtpRequestState
import com.uj.appstorysautopaymanager.domain.auth.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SendOtpUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(phoneNumber: String, activity: Activity): Flow<OtpRequestState> =
        repository.sendOtp(phoneNumber, activity)
}
