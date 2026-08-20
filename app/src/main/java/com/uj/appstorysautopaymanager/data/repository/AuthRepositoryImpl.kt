package com.uj.appstorysautopaymanager.data.repository

import android.app.Activity
import com.uj.appstorysautopaymanager.data.local.dao.AuthTokenDao
import com.uj.appstorysautopaymanager.data.local.entity.AuthTokenEntity
import com.uj.appstorysautopaymanager.data.remote.FirebasePhoneAuthDataSource
import com.uj.appstorysautopaymanager.domain.auth.model.AuthUser
import com.uj.appstorysautopaymanager.domain.auth.model.OtpRequestState
import com.uj.appstorysautopaymanager.domain.auth.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val remote: FirebasePhoneAuthDataSource,
    private val tokenDao: AuthTokenDao
) : AuthRepository {

    override fun sendOtp(phoneNumber: String, activity: Activity): Flow<OtpRequestState> =
        remote.sendOtp(phoneNumber, activity)

    override suspend fun verifyOtp(verificationId: String, code: String): Result<AuthUser> = runCatching {
        val firebaseUser = remote.verifyCode(verificationId, code)
        val idToken = firebaseUser.getIdToken(false).await().token.orEmpty()
        val user = AuthUser(
            uid = firebaseUser.uid,
            phoneNumber = firebaseUser.phoneNumber.orEmpty(),
            idToken = idToken
        )
        tokenDao.saveToken(
            AuthTokenEntity(
                uid = user.uid,
                phoneNumber = user.phoneNumber,
                idToken = user.idToken,
                issuedAt = System.currentTimeMillis()
            )
        )
        user
    }

    override suspend fun getStoredUser(): AuthUser? = tokenDao.getToken()?.let {
        AuthUser(uid = it.uid, phoneNumber = it.phoneNumber, idToken = it.idToken)
    }

    override suspend fun signOut() {
        remote.signOut()
        tokenDao.clear()
    }
}
