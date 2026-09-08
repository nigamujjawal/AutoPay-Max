package com.uj.appstorysautopaymanager.ui.auth

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uj.appstorysautopaymanager.data.local.pref.PreferenceManager
import com.uj.appstorysautopaymanager.data.remote.SessionExpiredNotifier
import com.uj.appstorysautopaymanager.domain.auth.usecase.GetStoredUserUseCase
import com.uj.appstorysautopaymanager.domain.auth.usecase.SignInWithGoogleUseCase
import com.uj.appstorysautopaymanager.domain.auth.usecase.SignOutUseCase
import com.uj.appstorysautopaymanager.util.GmailAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// One-shot outcomes of the connect flow, consumed by GoogleConnectScreen. Gmail authorization is
// optional/non-fatal - Completed fires whether or not the read scope was granted.
sealed interface ConnectEvent {
    data class NeedsGmailResolution(val pendingIntent: android.app.PendingIntent) : ConnectEvent
    data object Completed : ConnectEvent
    data class Failed(val message: String) : ConnectEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val getStoredUserUseCase: GetStoredUserUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val sessionExpiredNotifier: SessionExpiredNotifier,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // MainActivity's nav host collects this to force navigation back to the connect screen from
    // wherever the user is - a 401 can arrive on any screen.
    val sessionExpired: SharedFlow<Unit> = sessionExpiredNotifier.events

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _connectEvents = MutableSharedFlow<ConnectEvent>(extraBufferCapacity = 1)
    val connectEvents: SharedFlow<ConnectEvent> = _connectEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            _isAuthenticated.value = getStoredUserUseCase() != null
        }
        viewModelScope.launch {
            sessionExpiredNotifier.events.collect {
                Log.d("AuthFlow", "sessionExpired received - forcing signOut() back to connect screen")
                signOut()
            }
        }
    }

    // Sign in with Google (Credential Manager), then grab the Gmail read scope - one user action.
    fun connect(activity: Activity) {
        if (_isConnecting.value) return
        _isConnecting.value = true
        viewModelScope.launch {
            val user = signInWithGoogleUseCase(activity).getOrElse { e ->
                Log.e("AuthFlow", "Google sign-in failed", e)
                _isConnecting.value = false
                _connectEvents.emit(ConnectEvent.Failed(e.message ?: "Google sign-in failed, please try again"))
                return@launch
            }
            _isAuthenticated.value = true

            when (val r = GmailAuthManager.authorize(activity)) {
                is GmailAuthManager.AuthResult.Granted -> {
                    preferenceManager.markGmailConnected(user.email)
                    complete()
                }
                is GmailAuthManager.AuthResult.NeedsResolution ->
                    _connectEvents.emit(ConnectEvent.NeedsGmailResolution(r.pendingIntent)) // stays "connecting" until the result
                else -> complete() // Gmail denied/failed - proceed without it
            }
        }
    }

    fun onGmailResolutionResult(data: Intent?, activity: Activity) {
        viewModelScope.launch {
            if (GmailAuthManager.resultFromIntent(activity, data) is GmailAuthManager.AuthResult.Granted) {
                preferenceManager.markGmailConnected(preferenceManager.signedInEmailFlow.first())
            }
            complete()
        }
    }

    private suspend fun complete() {
        _isConnecting.value = false
        _connectEvents.emit(ConnectEvent.Completed)
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            _isAuthenticated.value = false
        }
    }
}
