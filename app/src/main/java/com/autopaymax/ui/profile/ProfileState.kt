package com.autopaymax.ui.profile

import com.autopaymax.domain.profile.model.UserProfile

data class ProfileState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val profile: UserProfile? = null
)
