package com.uj.appstorysautopaymanager.domain.auth.repository

import android.app.Activity
import com.uj.appstorysautopaymanager.domain.auth.model.AuthUser
import com.uj.appstorysautopaymanager.domain.auth.model.OtpRequestState
import kotlinx.coroutines.flow.Flow

// Activity is an unavoidable, deliberate compromise: Firebase's PhoneAuthOptions requires one to
// attach the reCAPTCHA/auto-retrieval flow to. Not worth abstracting away for this.
interface AuthRepository {
    fun sendOtp(phoneNumber: String, activity: Activity): Flow<OtpRequestState>
    suspend fun verifyOtp(verificationId: String, code: String): Result<AuthUser>
    suspend fun getStoredUser(): AuthUser?
    suspend fun signOut()
}
