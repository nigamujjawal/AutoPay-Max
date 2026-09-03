package com.uj.appstorysautopaymanager.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uj.appstorysautopaymanager.domain.auth.model.OtpRequestState
import com.uj.appstorysautopaymanager.domain.auth.usecase.GetStoredUserUseCase
import com.uj.appstorysautopaymanager.domain.auth.usecase.SendOtpUseCase
import com.uj.appstorysautopaymanager.domain.auth.usecase.SignOutUseCase
import com.uj.appstorysautopaymanager.domain.auth.usecase.VerifyOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginStep {
    data object EnterPhone : LoginStep
    data class EnterOtp(val phoneNumber: String, val verificationId: String) : LoginStep
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sendOtpUseCase: SendOtpUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val getStoredUserUseCase: GetStoredUserUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _step = MutableStateFlow<LoginStep>(LoginStep.EnterPhone)
    val step: StateFlow<LoginStep> = _step.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            _isAuthenticated.value = getStoredUserUseCase() != null
        }
    }

    fun sendOtp(phoneNumber: String, activity: Activity) {
        _errorMessage.value = null
        _isLoading.value = true
        viewModelScope.launch {
            sendOtpUseCase(phoneNumber, activity).collect { state ->
                when (state) {
                    is OtpRequestState.CodeSent -> {
                        _isLoading.value = false
                        _step.value = LoginStep.EnterOtp(phoneNumber, state.verificationId)
                    }
                    is OtpRequestState.AutoVerified -> {
                        _isLoading.value = false
                        _isAuthenticated.value = true
                    }
                    is OtpRequestState.Failed -> {
                        _isLoading.value = false
                        _errorMessage.value = state.message
                    }
                }
            }
        }
    }

    fun resendOtp(activity: Activity) {
        val current = _step.value
        if (current is LoginStep.EnterOtp) sendOtp(current.phoneNumber, activity)
    }

    fun verifyOtp(code: String) {
        val current = _step.value
        if (current !is LoginStep.EnterOtp) return
        _errorMessage.value = null
        _isLoading.value = true
        viewModelScope.launch {
            verifyOtpUseCase(current.verificationId, code)
                .onSuccess {
                    _isLoading.value = false
                    _isAuthenticated.value = true
                }
                .onFailure { e ->
                    _isLoading.value = false
                    _errorMessage.value = e.message ?: "Incorrect code, please try again"
                }
        }
    }

    fun backToPhoneEntry() {
        _errorMessage.value = null
        _step.value = LoginStep.EnterPhone
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            _isAuthenticated.value = false
            _step.value = LoginStep.EnterPhone
        }
    }
}
