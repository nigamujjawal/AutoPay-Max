package com.uj.appstorysautopaymanager.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

// The one place that touches Google's authorization API for the gmail.readonly scope. No
// requestOfflineAccess() and no app-managed refresh token: the grant is cached at the
// Play-Services layer, so re-calling authorize() later (including from GmailSyncWorker, which
// only has a Context) silently re-issues a fresh access token while consent is still valid. The
// token is never persisted - callers hold it in memory for one sync run.
object GmailAuthManager {

    const val GMAIL_READONLY_SCOPE = "https://www.googleapis.com/auth/gmail.readonly"

    sealed interface AuthResult {
        data class Granted(val accessToken: String) : AuthResult
        // First-consent (or re-consent) needs a UI the caller must launch via an Activity result.
        data class NeedsResolution(val pendingIntent: PendingIntent) : AuthResult
        // Same situation but reached from a headless worker with no Activity to resolve it.
        data object ReauthRequired : AuthResult
        data class Failed(val error: String) : AuthResult
    }

    private fun request() = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(GMAIL_READONLY_SCOPE)))
        .build()

    // Activity-context path (connect screen). A resolution PendingIntent is surfaced for launch.
    suspend fun authorize(context: Context): AuthResult = runCatching {
        toResult(Identity.getAuthorizationClient(context).authorize(request()).await(), allowResolution = true)
    }.getOrElse { AuthResult.Failed(it.message ?: "Gmail authorization failed") }

    // Worker path - no Activity, so a required resolution becomes ReauthRequired (soft no-op).
    suspend fun trySilentAuthorize(context: Context): AuthResult = runCatching {
        toResult(Identity.getAuthorizationClient(context).authorize(request()).await(), allowResolution = false)
    }.getOrElse { AuthResult.Failed(it.message ?: "Gmail authorization failed") }

    // Called from the connect screen's ActivityResult callback after the user completes consent.
    fun resultFromIntent(context: Context, data: Intent?): AuthResult = runCatching {
        val token = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data).accessToken
        if (token.isNullOrBlank()) AuthResult.Failed("No access token after consent") else AuthResult.Granted(token)
    }.getOrElse { AuthResult.Failed(it.message ?: "Gmail authorization failed") }

    private fun toResult(res: AuthorizationResult, allowResolution: Boolean): AuthResult {
        if (res.hasResolution()) {
            val pi = res.pendingIntent
            return if (allowResolution && pi != null) AuthResult.NeedsResolution(pi) else AuthResult.ReauthRequired
        }
        val token = res.accessToken
        return if (token.isNullOrBlank()) {
            Log.w("GmailAuth", "authorize returned no resolution and no token")
            AuthResult.Failed("No access token returned")
        } else {
            AuthResult.Granted(token)
        }
    }
}
