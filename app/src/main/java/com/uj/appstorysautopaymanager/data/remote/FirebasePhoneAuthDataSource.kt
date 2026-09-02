package com.uj.appstorysautopaymanager.data.remote

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.uj.appstorysautopaymanager.domain.auth.model.OtpRequestState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "AuthFlow"

class FirebasePhoneAuthDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun sendOtp(phoneNumber: String, activity: Activity, isResend: Boolean = false): Flow<OtpRequestState> =
        callbackFlow {
            val producerScope = this
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    Log.d(TAG, "onCodeSent verificationId=$verificationId")
                    resendToken = token
                    trySend(OtpRequestState.CodeSent(verificationId))
                }

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // SMS auto-retrieval fired - complete sign-in here so no PhoneAuthCredential
                    // (a Firebase type) ever has to cross into the domain layer.
                    Log.d(TAG, "onVerificationCompleted (auto-retrieval/instant verification fired)")
                    producerScope.launch {
                        // Was: runCatching { ... } with the Result thrown away, so a real
                        // signInWithCredential failure (expired/invalid credential, network error)
                        // still unconditionally sent AutoVerified - the repository would then find
                        // no signed-in FirebaseUser and report a generic failure, or worse, exchange
                        // a stale previous session. Report the real outcome instead.
                        runCatching { firebaseAuth.signInWithCredential(credential).await() }
                            .onSuccess {
                                Log.d(TAG, "signInWithCredential (auto) succeeded uid=${it.user?.uid}")
                                trySend(OtpRequestState.AutoVerified)
                            }
                            .onFailure {
                                Log.e(TAG, "signInWithCredential (auto) failed", it)
                                trySend(OtpRequestState.Failed(it.message ?: "Auto sign-in failed"))
                            }
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e(TAG, "onVerificationFailed", e)
                    trySend(OtpRequestState.Failed(e.message ?: "Verification failed"))
                }
            }

            val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
            if (isResend) {
                resendToken?.let { optionsBuilder.setForceResendingToken(it) }
            }
            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())

            awaitClose { }
        }

    suspend fun verifyCode(verificationId: String, code: String): FirebaseUser {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        val result = firebaseAuth.signInWithCredential(credential).await()
        return result.user ?: throw IllegalStateException("Sign-in succeeded but no user was returned")
    }

    fun signOut() = firebaseAuth.signOut()

    // Auto-retrieval (onVerificationCompleted) signs the user into Firebase right here, before the
    // repository ever sees it - this is how the repository reaches that same freshly-signed-in
    // user afterwards to run the backend exchange.
    fun currentUser(): FirebaseUser? = firebaseAuth.currentUser
}
