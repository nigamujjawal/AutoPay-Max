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
class AuthInterceptor @Inject constructor(
    private val tokenDao: AuthTokenDao,
    private val firebaseAuth: FirebaseAuth,
    // Provider, not AutoPayApi directly: AutoPayApi's own Retrofit/OkHttpClient graph depends on
    // this interceptor, so a direct dependency here would be circular. Provider defers the actual
    // lookup to refresh time, by which point the graph already exists.
    private val api: Provider<AutoPayApi>,
    private val sessionExpiredNotifier: SessionExpiredNotifier
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

        // No token was ever attached - this isn't a session that expired, it's a call that had no
        // session to begin with (a background worker running before/without login, most likely).
        // There's nothing to refresh and nothing to expire; forcing a navigate-to-Login here would
        // just clobber whatever screen is actually on-screen right now (including the login flow
        // itself, mid-OTP-entry, which is exactly what was happening before this check existed).
        if (accessToken.isNullOrBlank()) {
            Log.d("AuthFlow", "401 on ${request.url.encodedPath} with no access token attached - not a session expiry, ignoring")
            return response
        }

        Log.w("AuthFlow", "401 on ${request.url.encodedPath} (hadAccessToken=true) - attempting Firebase refresh")

        // Access token expired. The backend has no /auth/refresh route (confirmed against its API
        // doc - only /auth/firebase, /payments, /users, /subscriptions exist), so the stored
        // refresh_token field has nothing to be spent on. The actual refresh mechanism is
        // Firebase's own session: force a new Firebase ID token (silent as long as the Firebase
        // sign-in itself hasn't been revoked - no OTP re-entry) and re-exchange it on the same
        // public endpoint used at login, instead of forcing the user back to the login screen for
        // an access token that's simply due for renewal.
        val newAccessToken = firebaseAuth.currentUser?.let { refreshAccessToken(it) }
        if (newAccessToken == null) {
            Log.e("AuthFlow", "refresh failed (firebaseUser=${firebaseAuth.currentUser?.uid}) - forcing logout")
            forceLogout()
            return response
        }

        response.close()
        val retriedResponse = chain.proceed(
            authorizedRequest.newBuilder().header("Authorization", "Bearer $newAccessToken").build()
        )
        // A fresh token still got rejected - not an expiry, something is actually wrong with the
        // session (e.g. revoked backend-side). No more retries past this point.
        if (retriedResponse.code == 401) {
            Log.e("AuthFlow", "retry with freshly-refreshed token still got 401 - forcing logout")
            forceLogout()
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

    // Room has no way to know the backend has actually given up on this session - clearing here
    // (not just broadcasting) keeps the local session consistent immediately even if nothing is
    // around to react to the notification right now (e.g. a background sync).
    private fun forceLogout() {
        runBlocking { tokenDao.clear() }
        sessionExpiredNotifier.notifyExpired()
    }
}
