package com.uj.appstorysautopaymanager.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uj.appstorysautopaymanager.data.local.pref.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    val pinCode: StateFlow<String> = preferenceManager.pinCodeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val isBiometricEnabled: StateFlow<Boolean> = preferenceManager.isBiometricEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    fun checkPin(pin: String): Boolean {
        val matches = pin == pinCode.value
        if (matches) {
            _isAuthenticated.value = true
        }
        return matches
    }

    fun setAuthenticated(value: Boolean) {
        _isAuthenticated.value = value
    }

    fun isSecurityActive(): Boolean {
        return pinCode.value.isNotEmpty()
    }
}
