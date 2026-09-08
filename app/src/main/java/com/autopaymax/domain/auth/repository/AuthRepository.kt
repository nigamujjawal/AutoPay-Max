package com.autopaymax.domain.auth.repository

import android.app.Activity
import com.autopaymax.domain.auth.model.AuthUser

// Activity is an unavoidable, deliberate compromise: Credential Manager needs one to anchor the
// account-picker bottom sheet to. Not worth abstracting away for this.
interface AuthRepository {
    suspend fun signInWithGoogle(activity: Activity): Result<AuthUser>
    suspend fun getStoredUser(): AuthUser?
    // Whether a usable SoundBox backend access token exists (it's optional - see AuthRepositoryImpl).
    suspend fun hasBackendSession(): Boolean
    suspend fun signOut()
}
