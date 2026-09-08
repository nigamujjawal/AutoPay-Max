package com.uj.appstorysautopaymanager.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.uj.appstorysautopaymanager.data.local.dao.AuthTokenDao
import com.uj.appstorysautopaymanager.data.remote.dto.LoginRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider

// Runs on OkHttp's dispatcher thread (never the main thread), so blocking on Room/network here is
// safe. /auth/firebase is the one public route - it's what produces the access token in the first
// place, so it must never carry a (possibly stale) Bearer header.
//
// The SoundBox backend session is OPTIONAL (see AuthRepositoryImpl) - a failed refresh drops the
// local backend token but never forces the user back to sign-in. Backend calls just no-op / fail
// quietly from then on; Gmail sync (on-device) is unaffected.
class AuthInterceptor @Inject constructor(
    private val tokenDao: AuthTokenDao,
    private val firebaseAuth: FirebaseAuth,
    // Provider, not AutoPayApi directly: AutoPayApi's own Retrofit/OkHttpClient graph depends on
    // this interceptor, so a direct dependency here would be circular.
    private val api: Provider<AutoPayApi>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.encodedPath.endsWith("/auth/firebase")) {
            return chain.proceed(request)
        }

        val accessToken = runBlocking { tokenDao.getToken()?.accessToken }
        val authorizedRequest = if (!accessToken.isNullOrBlank()) {
            request.newBuilder().addHeader("Authorization", "Bearer $accessToken").build()
        } else {
            request
        }
        val response = chain.proceed(authorizedRequest)
        if (response.code != 401) return response

        // No token was ever attached - a call that had no backend session to begin with (a
        // Google user the backend can't onboard, or a worker running before sign-in). Nothing to
        // refresh, nothing to expire.
        if (accessToken.isNullOrBlank()) {
            Log.d("AuthFlow", "401 on ${request.url.encodedPath} with no access token - backend session absent, ignoring")
            return response
        }

        Log.w("AuthFlow", "401 on ${request.url.encodedPath} (hadAccessToken=true) - attempting Firebase refresh")

        // Access token expired. The backend has no /auth/refresh route - the actual refresh is
        // Firebase's own session: force a new Firebase ID token and re-exchange it on the public
        // /auth/firebase endpoint. For a returning backend user this succeeds silently; if it
        // fails, drop the backend session (but stay signed in).
        val newAccessToken = firebaseAuth.currentUser?.let { refreshAccessToken(it) }
        if (newAccessToken == null) {
            Log.w("AuthFlow", "backend refresh failed - dropping backend session (user stays signed in)")
            dropBackendSession()
            return response
        }

        response.close()
        val retriedResponse = chain.proceed(
            authorizedRequest.newBuilder().header("Authorization", "Bearer $newAccessToken").build()
        )
        if (retriedResponse.code == 401) {
            Log.w("AuthFlow", "retry with a freshly-refreshed token still got 401 - dropping backend session")
            dropBackendSession()
        } else {
            Log.d("AuthFlow", "refresh succeeded, retried request got ${retriedResponse.code}")
        }
        return retriedResponse
    }

    private fun refreshAccessToken(firebaseUser: FirebaseUser): String? = runCatching {
        runBlocking {
            val newIdToken = firebaseUser.getIdToken(true).await().token.orEmpty()
            val session = api.get().firebaseLogin(LoginRequest(id_token = newIdToken))
            val existing = tokenDao.getToken() ?: return@runBlocking null
            tokenDao.saveToken(
                existing.copy(
                    idToken = newIdToken,
                    accessToken = session.access_token,
                    refreshToken = session.refresh_token,
                    issuedAt = System.currentTimeMillis()
                )
            )
            session.access_token
        }
    }.getOrNull()

    // Blank the backend token but keep the auth_token row - the user is still signed in via
    // Firebase/Google, they just have no SoundBox session right now.
    private fun dropBackendSession() {
        runBlocking {
            val existing = tokenDao.getToken() ?: return@runBlocking
            tokenDao.saveToken(existing.copy(accessToken = "", refreshToken = ""))
        }
    }
}
