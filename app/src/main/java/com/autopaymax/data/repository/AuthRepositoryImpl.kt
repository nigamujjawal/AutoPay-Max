package com.autopaymax.data.repository

import android.app.Activity
import android.util.Log
import com.appversal.appstorys.AppStorys
import com.google.firebase.auth.FirebaseUser
import com.autopaymax.data.local.dao.AuthTokenDao
import com.autopaymax.data.local.entity.AuthTokenEntity
import com.autopaymax.data.local.pref.PreferenceManager
import com.autopaymax.data.remote.GoogleAuthDataSource
import com.autopaymax.data.remote.AutoPayApi
import com.autopaymax.data.remote.dto.LoginRequest
import com.autopaymax.domain.auth.model.AuthUser
import com.autopaymax.domain.auth.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val remote: GoogleAuthDataSource,
    private val api: AutoPayApi,
    private val tokenDao: AuthTokenDao,
    private val preferenceManager: PreferenceManager
) : AuthRepository {

    // In-app Google account picker -> Firebase (federated). The SoundBox backend session is
    // OPTIONAL: /auth/firebase's create-user path rejects a Google-federated token (no phone
    // number -> HTTP 400), so we take the backend session when it's available and carry on
    // without it when it isn't. Gmail sync is fully on-device; the only things that need the
    // backend token (PaymentSyncWorker, backend Profile) degrade to a no-op.
    override suspend fun signInWithGoogle(activity: Activity): Result<AuthUser> = runCatching {
        federate(remote.signIn(activity))
    }

    private suspend fun federate(firebaseUser: FirebaseUser): AuthUser {
        val idToken = firebaseUser.getIdToken(false).await().token.orEmpty()
        val user = AuthUser(
            uid = firebaseUser.uid,
            email = firebaseUser.email.orEmpty(),
            idToken = idToken
        )

        // Best-effort backend exchange - null (and a blank access token stored below) if the
        // backend can't onboard this user.
        val session = runCatching { api.firebaseLogin(LoginRequest(id_token = idToken)) }
            .onFailure { Log.w("AuthFlow", "backend /auth/firebase exchange failed - continuing without a backend session", it) }
            .getOrNull()

        // saveToken REPLACEs the whole row - carry over the locally-set name across a re-login
        // for the same uid, or it silently resets to "" every time this runs.
        val existingName = tokenDao.getToken()?.takeIf { it.uid == user.uid }?.name.orEmpty()
        tokenDao.saveToken(
            AuthTokenEntity(
                uid = user.uid,
                phoneNumber = "",
                email = user.email,
                idToken = user.idToken,
                accessToken = session?.access_token.orEmpty(),
                refreshToken = session?.refresh_token.orEmpty(),
                issuedAt = System.currentTimeMillis(),
                name = existingName
            )
        )
        preferenceManager.setSignedInEmail(user.email)
        // AppStorys was initialized with a placeholder id (Application.onCreate() runs before any
        // user is signed in) - identify the real user as soon as one actually logs in.
        AppStorys.setUserId(user.uid)
        return user
    }

    // Signed in = we have a Firebase uid on file. The backend access token may legitimately be
    // blank (Google user the backend can't onboard) - that's not "not signed in".
    override suspend fun getStoredUser(): AuthUser? = tokenDao.getToken()
        ?.takeIf { it.uid.isNotBlank() }
        ?.let { AuthUser(uid = it.uid, email = it.email, idToken = it.idToken) }

    // True only when a usable SoundBox backend session exists - background workers that hit the
    // backend gate on this, not on getStoredUser().
    override suspend fun hasBackendSession(): Boolean =
        tokenDao.getToken()?.accessToken?.isNotBlank() == true

    override suspend fun signOut() {
        remote.signOut()
        tokenDao.clear()
        preferenceManager.setSignedInEmail("")
        preferenceManager.clearGmailConnection()
    }
}
