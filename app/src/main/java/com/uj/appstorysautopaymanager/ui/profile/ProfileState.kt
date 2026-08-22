package com.uj.appstorysautopaymanager.ui.profile

import com.uj.appstorysautopaymanager.domain.profile.model.UserProfile

data class ProfileState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val profile: UserProfile? = null
)
