package com.autopaymax.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autopaymax.common.Resource
import com.autopaymax.domain.profile.usecase.GetUserProfileUseCase
import com.autopaymax.domain.profile.usecase.UpdateUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserProfile: GetUserProfileUseCase,
    private val updateUserProfile: UpdateUserProfileUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<ProfileUiEvent>()
    val event: SharedFlow<ProfileUiEvent> = _event.asSharedFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = getUserProfile()) {
                is Resource.Success -> _state.update { it.copy(isLoading = false, profile = result.data) }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false) }
                    _event.emit(ProfileUiEvent.ShowMessage(result.message ?: "Failed to load profile"))
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun saveProfile(name: String? = null, upiId: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            when (val result = updateUserProfile(name, upiId)) {
                is Resource.Success -> {
                    _state.update { it.copy(isSaving = false) }
                    _event.emit(ProfileUiEvent.ShowMessage("Profile updated"))
                    loadProfile()
                }
                is Resource.Error -> {
                    _state.update { it.copy(isSaving = false) }
                    _event.emit(ProfileUiEvent.ShowMessage(result.message ?: "Failed to update profile"))
                }
                is Resource.Loading -> Unit
            }
        }
    }
}
