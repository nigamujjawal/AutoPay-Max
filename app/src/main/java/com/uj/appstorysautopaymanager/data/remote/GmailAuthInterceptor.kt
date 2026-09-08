package com.uj.appstorysautopaymanager.data.remote

import okhttp3.Interceptor
import okhttp3.Response

// The Gmail access token is short-lived and scoped to a single GmailSyncWorker run - never
// persisted (unlike AuthInterceptor, which reads a DAO). The worker sets this before its calls
// and clears it in a finally block.
object GmailTokenHolder {
    @Volatile
    var accessToken: String? = null
}

class GmailAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = GmailTokenHolder.accessToken
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        // No 401-refresh loop: a 401 here means this run's token was already bad, not expired -
        // just let the call fail, the next periodic run re-authorizes from scratch.
        return chain.proceed(request)
    }
}
