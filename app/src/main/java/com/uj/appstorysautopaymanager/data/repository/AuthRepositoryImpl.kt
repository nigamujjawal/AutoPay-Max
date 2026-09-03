package com.uj.appstorysautopaymanager.data.repository

import android.app.Activity
import android.util.Log
import com.appversal.appstorys.AppStorys
import com.google.firebase.auth.FirebaseUser
import com.uj.appstorysautopaymanager.data.local.dao.AuthTokenDao
import com.uj.appstorysautopaymanager.data.local.entity.AuthTokenEntity
import com.uj.appstorysautopaymanager.data.remote.FirebasePhoneAuthDataSource
import com.uj.appstorysautopaymanager.data.remote.AutoPayApi
import com.uj.appstorysautopaymanager.data.remote.dto.LoginRequest
import com.uj.appstorysautopaymanager.domain.auth.model.AuthUser
import com.uj.appstorysautopaymanager.domain.auth.model.OtpRequestState
import com.uj.appstorysautopaymanager.domain.auth.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val remote: FirebasePhoneAuthDataSource,
    private val api: AutoPayApi,
    private val tokenDao: AuthTokenDao
) : AuthRepository {

    // SMS auto-retrieval (onVerificationCompleted) signs the user into Firebase and emits
    // AutoVerified without ever going through verifyOtp() below - that path used to skip the
    // backend exchange entirely, leaving Room with no access token and the very next API call
    //401ing with nothing to recover from. Running the same exchange here closes that gap.
    override fun sendOtp(phoneNumber: String, activity: Activity): Flow<OtpRequestState> =
        remote.sendOtp(phoneNumber, activity).map { state ->
            Log.d("AuthFlow", "repository received state=$state")
            if (state !is OtpRequestState.AutoVerified) return@map state
            val firebaseUser = remote.currentUser()
                ?: return@map OtpRequestState.Failed("Sign-in succeeded but no user was returned")
            exchangeFirebaseSession(firebaseUser)
                .onFailure { Log.e("AuthFlow", "auto-verify backend exchange failed", it) }
                .fold(onSuccess = { state }, onFailure = { OtpRequestState.Failed(it.message ?: "Sign-in failed") })
        }

    override suspend fun verifyOtp(verificationId: String, code: String): Result<AuthUser> {
        val firebaseUser = remote.verifyCode(verificationId, code)
        return exchangeFirebaseSession(firebaseUser)
    }

    // Every other SoundBox endpoint is authorized against the backend's own access token, not the
    // Firebase one - exchange it here (the one place both login paths funnel through) so the rest
    // of the app never has to.
    private suspend fun exchangeFirebaseSession(firebaseUser: FirebaseUser): Result<AuthUser> = runCatching {
        val idToken = firebaseUser.getIdToken(false).await().token.orEmpty()
        val session = api.firebaseLogin(LoginRequest(id_token = idToken))

        val user = AuthUser(
            uid = firebaseUser.uid,
            phoneNumber = firebaseUser.phoneNumber.orEmpty(),
            idToken = idToken
        )
        // saveToken REPLACEs the whole row - carry over the locally-set name across a re-login
        // for the same uid, or it would silently reset back to "" every time this runs.
        val existingName = tokenDao.getToken()?.takeIf { it.uid == user.uid }?.name.orEmpty()
        tokenDao.saveToken(
            AuthTokenEntity(
                uid = user.uid,
                phoneNumber = user.phoneNumber,
                idToken = user.idToken,
                accessToken = session.access_token,
                refreshToken = session.refresh_token,
                issuedAt = System.currentTimeMillis(),
                name = existingName
            )
        )
        // AppStorys was initialized with a placeholder/anonymous id (Application.onCreate() runs
        // before any user is signed in) - identify the real user as soon as one actually logs in.
        AppStorys.setUserId(user.uid)
        user
    }

    // accessToken can be blank for a session saved before the SoundBox backend was wired up
    // (pre-migration rows were backfilled with '') - treat that as not signed in, not just "no
    // backend token yet", so the user gets routed back to login and re-exchanges it via
    // verifyOtp() instead of every backend call failing with "missing authorization header" forever.
    override suspend fun getStoredUser(): AuthUser? = tokenDao.getToken()
        ?.takeIf { it.accessToken.isNotBlank() }
        ?.let { AuthUser(uid = it.uid, phoneNumber = it.phoneNumber, idToken = it.idToken) }

    override suspend fun signOut() {
        remote.signOut()
        tokenDao.clear()
    }
}
