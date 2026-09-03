package com.uj.appstorysautopaymanager.data.remote

import com.uj.appstorysautopaymanager.data.local.dao.AuthTokenDao
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

// Runs on OkHttp's dispatcher thread (never the main thread), so blocking on the Room read here
// is safe. /auth/firebase is the one public route - it's what produces the access token in the
// first place, so it must never carry a (possibly stale) Bearer header.
class AuthInterceptor @Inject constructor(
    private val tokenDao: AuthTokenDao
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
        return chain.proceed(authorizedRequest)
    }
}
